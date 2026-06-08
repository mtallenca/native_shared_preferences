// Copyright 2019 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package yeniellandestoy.native_shared_preferences

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.math.BigInteger
import java.util.concurrent.Executors

/**
 * Implementation of the [MethodChannel.MethodCallHandler] for the plugin. It is also
 * responsible for managing the [SharedPreferences].
 */
internal class MethodCallHandlerImpl(private val context: Context) :
    MethodChannel.MethodCallHandler {

    private val preferences: SharedPreferences = run {
        try {
            val resourceName = getResourceFromContext(context, "flutter_shared_pref_name")
            Log.d("SharedPreferences:", "using custom resource name - $resourceName")
            context.getSharedPreferences(resourceName, Context.MODE_PRIVATE)
        } catch (e: IllegalArgumentException) {
            Log.d("SharedPreferences:", "using default resource name")
            context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
        }
    }

    // Commits run on a single background thread, mirroring AsyncTask's serial
    // executor, and results are delivered back on the main thread.
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        val key = call.argument<String>("key")
        try {
            when (call.method) {
                "setBool" ->
                    commitAsync(preferences.edit().putBoolean(key, call.argument<Boolean>("value")!!), result)
                "setDouble" -> {
                    val doubleValue = call.argument<Number>("value")!!.toDouble()
                    val doubleValueStr = doubleValue.toString()
                    commitAsync(preferences.edit().putString(key, DOUBLE_PREFIX + doubleValueStr), result)
                }
                "setInt" -> {
                    val number = call.argument<Number>("value")
                    if (number is BigInteger) {
                        commitAsync(
                            preferences.edit().putString(
                                key, BIG_INTEGER_PREFIX + number.toString(Character.MAX_RADIX)
                            ),
                            result
                        )
                    } else {
                        commitAsync(preferences.edit().putLong(key, number!!.toLong()), result)
                    }
                }
                "setString" -> {
                    val value = call.argument<String>("value")!!
                    if (value.startsWith(LIST_IDENTIFIER) || value.startsWith(BIG_INTEGER_PREFIX)) {
                        result.error(
                            "StorageError",
                            "This string cannot be stored as it clashes with special identifier prefixes.",
                            null
                        )
                        return
                    }
                    commitAsync(preferences.edit().putString(key, value), result)
                }
                "setStringList" -> {
                    val list = call.argument<List<String>>("value")!!
                    commitAsync(preferences.edit().putString(key, LIST_IDENTIFIER + encodeList(list)), result)
                }
                "commit" ->
                    // We've been committing the whole time.
                    result.success(true)
                "getAll" -> {
                    result.success(getAllPrefs())
                    return
                }
                "remove" ->
                    commitAsync(preferences.edit().remove(key), result)
                "getAllFromDictionary" -> {
                    val keys = call.argument<List<String>>("keys")!!
                    result.success(getAllPrefsFromDictionaries(keys))
                }
                "clear" -> {
                    val keySet = getAllPrefs().keys
                    val clearEditor = preferences.edit()
                    for (keyToDelete in keySet) {
                        clearEditor.remove(keyToDelete)
                    }
                    commitAsync(clearEditor, result)
                }
                else -> result.notImplemented()
            }
        } catch (e: IOException) {
            result.error("IOException encountered", call.method, e)
        }
    }

    private fun commitAsync(editor: SharedPreferences.Editor, result: MethodChannel.Result) {
        executor.execute {
            val committed = editor.commit()
            mainHandler.post { result.success(committed) }
        }
    }

    @Throws(IOException::class)
    private fun decodeList(encodedList: String): List<String> =
        ObjectInputStream(ByteArrayInputStream(Base64.decode(encodedList, 0))).use { stream ->
            try {
                @Suppress("UNCHECKED_CAST")
                stream.readObject() as List<String>
            } catch (e: ClassNotFoundException) {
                throw IOException(e)
            }
        }

    @Throws(IOException::class)
    private fun encodeList(list: List<String>): String {
        val byteStream = ByteArrayOutputStream()
        ObjectOutputStream(byteStream).use { stream ->
            stream.writeObject(list)
            stream.flush()
        }
        return Base64.encodeToString(byteStream.toByteArray(), 0)
    }

    // Filter preferences to only those set by the flutter app.
    @Throws(IOException::class)
    private fun getAllPrefs(): Map<String, Any?> = parsePreferences(preferences.all)

    @Throws(IOException::class)
    private fun getAllPrefsFromDictionaries(keys: List<String>): Map<String, Any?> {
        val filteredPrefs = HashMap<String, Any?>()
        for (key in keys) {
            val allPrefs = context.getSharedPreferences(key, Context.MODE_PRIVATE).all
            filteredPrefs.putAll(parsePreferences(allPrefs))
        }
        return filteredPrefs
    }

    @Throws(IOException::class)
    private fun parsePreferences(allPrefs: Map<String, *>): Map<String, Any?> {
        val filteredPrefs = HashMap<String, Any?>()
        for (key in allPrefs.keys) {
            var value: Any? = allPrefs[key]
            if (value is String) {
                value = when {
                    value.startsWith(LIST_IDENTIFIER) ->
                        decodeList(value.substring(LIST_IDENTIFIER.length))
                    value.startsWith(BIG_INTEGER_PREFIX) ->
                        BigInteger(value.substring(BIG_INTEGER_PREFIX.length), Character.MAX_RADIX)
                    value.startsWith(DOUBLE_PREFIX) ->
                        value.substring(DOUBLE_PREFIX.length).toDouble()
                    else -> value
                }
            } else if (value is Set<*>) {
                // This only happens for previous usage of setStringSet. The app expects a list.
                @Suppress("UNCHECKED_CAST")
                val listValue = ArrayList(value as Set<String>)
                // Let's migrate the value too while we are at it.
                val success = preferences
                    .edit()
                    .remove(key)
                    .putString(key, LIST_IDENTIFIER + encodeList(listValue))
                    .commit()
                if (!success) {
                    // If we are unable to migrate the existing preferences, it means we potentially lost them.
                    // In this case, an error from getAllPrefs() is appropriate since it will alert the app during plugin initialization.
                    throw IOException("Could not migrate set to list")
                }
                value = listValue
            }
            filteredPrefs[key] = value
        }
        return filteredPrefs
    }

    companion object {
        private const val SHARED_PREFERENCES_NAME = "FlutterSharedPreferences"

        // Fun fact: The following is a base64 encoding of the string "This is the prefix for a list."
        private const val LIST_IDENTIFIER = "VGhpcyBpcyB0aGUgcHJlZml4IGZvciBhIGxpc3Qu"
        private const val BIG_INTEGER_PREFIX = "VGhpcyBpcyB0aGUgcHJlZml4IGZvciBCaWdJbnRlZ2Vy"
        private const val DOUBLE_PREFIX = "VGhpcyBpcyB0aGUgcHJlZml4IGZvciBEb3VibGUu"

        private fun getResourceFromContext(context: Context, resName: String): String {
            val stringRes = context.resources.getIdentifier(resName, "string", context.packageName)
            require(stringRes != 0) {
                "The 'R.string.$resName' value it's not defined in your project's resources file."
            }
            return context.getString(stringRes)
        }
    }
}
