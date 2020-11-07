package com.example.bb_temperature

import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.util.Log
import org.junit.Test

import org.junit.Assert.*
import java.io.ByteArrayOutputStream
import kotlin.experimental.and

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    fun dialTest(){
        var s:CharSequence = "02"
        }
    @Test
    fun send() {
//        if (!connected) {
//            Toast.makeText(this@DeviceControlActivity, "not connected", Toast.LENGTH_SHORT).show()
//            return
//        }
        var str = "44"
        try {
            val msg: String
            val data: ByteArray
            val sb = StringBuilder()
            toHexString(sb, fromHexString(str)!!)
            toHexString(sb, "\r\n".toByteArray())
            msg = sb.toString()
            data = fromHexString(msg)!!
            val spn = SpannableStringBuilder(
                """
                     $msg
                     
                     """.trimIndent()
            )

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun fromHexString(s: CharSequence): ByteArray? {
        val buf = ByteArrayOutputStream()
        var b: Byte = 0
        var nibble = 0
        var pos = 0

        while(pos < s.length){
            if (nibble == 2) {
                buf.write(b.toInt())
                nibble = 0
                b = 0
            }
            System.out.println("$s[pos]"+pos)
            System.out.println("spos = "+s[pos].toInt())
            val c = s[pos].toInt()
            System.out.println("${c.toString()}")

            if (c >= '0'.toInt() && c <= '9'.toInt()) {
                nibble++
                b = (16.times(b)).toByte()
                b = (c - '0'.toInt().plus(b)).toByte()
                System.out.println("c is between zoron nine b = "+b)

            }
            if (c >= 'A'.toInt() && c <= 'F'.toInt()) {
                nibble++
                b = 16.times(b).toByte()
                b = ((c - 'A'.toInt() + 10).plus(b)).toByte()
                System.out.println("A is between zoron nine F = "+b)

            }
            if (c >= 'a'.toInt() && c <= 'f'.toInt()) {
                nibble++
                b = (16.times(b)).toByte()
                b = (c - 'a'.toInt() + 10).plus(b).toByte()
                System.out.println("a is between zoron nine f = "+b)

            }
            pos++
        }
        if (nibble > 0) buf.write(b.toInt())
        return buf.toByteArray()
    }

    @JvmOverloads
    fun toHexString(sb: StringBuilder, buf: ByteArray, begin: Int = 0, end: Int = buf.size) {

        var pos = begin
        while (pos < end){
            if (sb.length > 0) sb.append(' ')
            var c: Int
            c = (buf[pos] and 0xff.toByte()) / 16
            c += if (c >= 10) 'A'.toInt() - 10 else '0'.toInt()
            sb.append(c.toChar())
            c = (buf[pos] and 0xff.toByte()) % 16
            c += if (c >= 10) 'A'.toInt() - 10 else '0'.toInt()
            sb.append(c.toChar())
            pos++
        }
    }

    }
