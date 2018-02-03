package java.io;

public class StringReader extends Reader {
    private String str;
    private int length;
    private int next;

	public StringReader(String s) {
		str = s;
        length = s.length();
	}

	public int read(char cbuf[], int off, int len) {
		if ((off < 0) || (off > cbuf.length) || (len < 0) || ((off + len) > cbuf.length) || ((off + len) < 0)) {
			throw new IndexOutOfBoundsException();
		} else if (len == 0) {
			return 0;
		}
		if (next >= length)
			return -1;
		int n = Math.min(length - next, len);
		str.getChars(next, next + n, cbuf, off);
		next += n;
		return n;
	}
}