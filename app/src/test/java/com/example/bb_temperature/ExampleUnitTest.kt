package com.example.bb_temperature

import android.text.SpannableStringBuilder
import org.junit.Assert.assertEquals
import org.junit.Test
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
        var str = "98"
        try {
            val msg: String
            val data: ByteArray
            val sb = StringBuilder()
            toHexString(sb, fromHexString(str)!!)
            System.out.println("sb 1 ="+sb.toString());
            toHexString(sb, "\r\n".toByteArray())
            msg = sb.toString()
            System.out.println(msg);
            data = fromHexString(msg)!!
            val spn = SpannableStringBuilder(
                """
                     $msg
                     
                     """.trimIndent()
            )
            System.out.println("msg = " + msg)
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
            System.out.println("s length = " + s.length);
            System.out.println("pos length = " + pos);
            if (nibble == 2) {
                System.out.println("buf write b 1= " + b);
                buf.write(b.toInt())
                nibble = 0
                b = 0
            }
            val c = s.get(pos).toInt()
            System.out.println("int c = " + c);

            if (c >= '0'.toInt() && c <= '9'.toInt()) {
                nibble++
                b = (16.times(b)).toByte()
                System.out.println("b time 16 = " + b);
                System.out.println("c = " + c + " b = " + b + " 0 = " + (c - '0'.toInt()));
                b = ((c - '0'.toInt()).plus(b)).toByte()
                System.out.println("b between 0 - 9  = " + b);

            }
            if (c >= 'A'.toInt() && c <= 'F'.toInt()) {
                nibble++
                b = 16.times(b).toByte()
                b = ((c - 'A'.toInt() + 10).plus(b)).toByte()
                System.out.println("b between A - F  = " + b);

            }
            if (c >= 'a'.toInt() && c <= 'f'.toInt()) {
                nibble++
                b = (16.times(b)).toByte()
                b = (c - 'a'.toInt() + 10).plus(b).toByte()
                System.out.println("b between a - f  = " + b);

            }
            pos++
        }
        if (nibble > 0) {
            System.out.println("buf write b 2= " +b);
            buf.write(b.toInt())
        }
        System.out.println(buf.toString())
        return buf.toByteArray()
    }

    @JvmOverloads
    fun toHexString(sb: StringBuilder, buf: ByteArray, begin: Int = 0, end: Int = buf.size) {
        System.out.println("toHexString sb= "+sb.toString());
        System.out.println("toHexString buf= "+(buf[0]));
        var pos = begin
        while (pos < end){
            if (sb.length > 0) sb.append(' ')
            var c: Int
            System.out.println("toHexString buf[[pos]= "+buf[pos]);
            System.out.println("toHexString buf[[pos] bit and= "+(0xff.and(buf[pos].toInt())));
            c = (0xff.and(buf[pos].toInt())) / 16
            System.out.println("toHexString c1= "+c);
            c += if (c >= 10) 'A'.toInt() - 10 else '0'.toInt( )
            sb.append(c.toChar())
            System.out.println("toHexString c2= "+c);
            c = (0xff.and(buf[pos].toInt())) % 16
            c += if (c >= 10) 'A'.toInt() - 10 else '0'.toInt()
            System.out.println("toHexString c3= "+c);
            sb.append(c.toChar())
            pos++
        }
    }

    }


