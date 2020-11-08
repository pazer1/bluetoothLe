
import android.text.*
import android.text.style.BackgroundColorSpan
import android.util.Log
import android.widget.TextView
import androidx.annotation.ColorInt
import java.io.ByteArrayOutputStream
import kotlin.experimental.and

public class TextUtil {

    private val TAG:String = "TextUtil"

    companion object Create{
        fun create():TextUtil = TextUtil()
    }

    @ColorInt
    var caretBackground = -0x99999a
    val newline_crlf = "\r\n"
    val newline_lf = "\n"


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
    fun toHexString(buf: ByteArray, begin: Int = 0, end: Int = buf.size): String {
        val sb = StringBuilder(3 * (end - begin))
        toHexString(sb, buf, begin, end)
        return sb.toString()
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

    @JvmOverloads
    fun toCaretString(s: CharSequence, keepNewline: Boolean, length: Int = s.length): CharSequence {
        var found = false
        for (pos in 0 until length) {
            if (s[pos].toInt() < 32 && (!keepNewline || s[pos] != '\n')) {
                found = true
                break
            }
        }
        if (!found) return s
        val sb = SpannableStringBuilder()
        for (pos in 0 until length) if (s[pos].toInt() < 32 && (!keepNewline || s[pos] != '\n')) {
            sb.append('^')
            sb.append((s[pos].toInt() + 64).toChar())
            sb.setSpan(
                BackgroundColorSpan(caretBackground),
                sb.length - 2,
                sb.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        } else {
            sb.append(s[pos])
        }
        return sb
    }

    internal class HexWatcher(private val view: TextView) : TextWatcher {
        private val sb = StringBuilder()
        private var self = false
        private var enabled = false
        fun enable(enable: Boolean) {
            if (enable) {
                view.inputType =
                    InputType.TYPE_CLASS_TEXT + InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                view.inputType = InputType.TYPE_CLASS_TEXT + InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            }
            enabled = enable
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable) {
            if (!enabled || self) return
            sb.delete(0, sb.length)
            var i: Int
            i = 0
            while (i < s.length) {
                val c = s[i]
                if (c >= '0' && c <= '9') sb.append(c)
                if (c >= 'A' && c <= 'F') sb.append(c)
                if (c >= 'a' && c <= 'f') sb.append((c + 'A'.toInt() - 'a'.toInt()))
                i++
            }
            i = 2
            while (i < sb.length) {
                sb.insert(i, ' ')
                i += 3
            }
            val s2 = sb.toString()
            if (s2 != s.toString()) {
                self = true
                s.replace(0, s.length, s2)
                self = false
            }
        }
    }
}