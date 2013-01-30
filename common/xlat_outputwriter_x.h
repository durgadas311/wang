
MAP('-', 0x00|NONZERO)
MAP('y', 0x01)
MAP(' ', 0x02)
MAP('\b', 0x03)
MAP('q', 0x04)
MAP('p', 0x05)
MAP('=', 0x06)
MAP('j', 0x07)
// MAP(' ', 0x08)      // no op
MAP('/', 0x09)
//MAP(' ', 0x0a)       // no op
//MAP(' ', 0x0b)       // no op
MAP(',', 0x0c)
MAP(';', 0x0d)
MAP('f', 0x0e)
MAP('g', 0x0f)

MAP('w', 0x10)
MAP('s', 0x11)
//MAP('', 0x12)        // shift dn
//MAP('', 0x13)        // shift up
MAP('i', 0x14)
MAP('\'', 0x15)
MAP('.', 0x16)
SPMAP('[', 0x17)      // 1/2...
MAP('\r', 0x18)		// return-index
MAP('o', 0x19)
MAP('\n', 0x1a)		// index
MAP('\v', 0x1b)		// rev index
MAP('a', 0x1c)
MAP('r', 0x1d)
MAP('v', 0x1e)
MAP('m', 0x1f)

MAP('b', 0x20)
MAP('h', 0x21)
//MAP('+', 0x22)       // step x+
//MAP('+', 0x23)       // step x-
MAP('k', 0x24)
MAP('e', 0x25)
MAP('n', 0x26)
MAP('t', 0x27)
//MAP('', 0x28)        // print mode
MAP('l', 0x29)
//MAP('+', 0x2a)       // step y+
//MAP('+', 0x2b)       // step y-
MAP('c', 0x2c)
MAP('d', 0x2d)
MAP('u', 0x2e)
MAP('x', 0x2f)

MAP('9', 0x30)
MAP('0', 0x31)
//MAP('', 0x32)        // step x+y+
//MAP('', 0x33)        // step x-y+
MAP('6', 0x34)
MAP('5', 0x35)
MAP('2', 0x36)
MAP('z', 0x37)
//MAP('', 0x38)        // plot mode
MAP('4', 0x39)
//MAP('', 0x3a)        // step x+y-
//MAP('', 0x3b)        // step x-y-
MAP('8', 0x3c)
MAP('7', 0x3d)
MAP('3', 0x3e)
MAP('1', 0x3f)

// shifted versions...
MAP('_', 0x00|SHIFT|NONZERO)
MAP('Y', 0x01|SHIFT)
MAP(' ', 0x02|SHIFT)
//MAP('\b', 0x03|SHIFT)
MAP('Q', 0x04|SHIFT)
MAP('P', 0x05|SHIFT)
MAP('+', 0x06|SHIFT)
MAP('J', 0x07|SHIFT)
MAP('?', 0x09|SHIFT)
MAP(',', 0x0c|SHIFT)
MAP(':', 0x0d|SHIFT)
MAP('F', 0x0e|SHIFT)
MAP('G', 0x0f|SHIFT)

MAP('W', 0x10|SHIFT)
MAP('S', 0x11|SHIFT)
MAP('I', 0x14|SHIFT)
MAP('"', 0x15|SHIFT)
MAP('.', 0x16|SHIFT)
SPMAP('{', 0x17|SHIFT)      // 1/4
//MAP('\n', 0x18|SHIFT)
MAP('O', 0x19|SHIFT)
//MAP('\n', 0x1a|SHIFT)
//MAP('\n', 0x1b|SHIFT)      // rev index
MAP('A', 0x1c|SHIFT)
MAP('R', 0x1d|SHIFT)
MAP('V', 0x1e|SHIFT)
MAP('M', 0x1f|SHIFT)

MAP('B', 0x20|SHIFT)
MAP('H', 0x21|SHIFT)
MAP('K', 0x24|SHIFT)
MAP('E', 0x25|SHIFT)
MAP('N', 0x26|SHIFT)
MAP('T', 0x27|SHIFT)
MAP('L', 0x29|SHIFT)
MAP('C', 0x2c|SHIFT)
MAP('D', 0x2d|SHIFT)
MAP('U', 0x2e|SHIFT)
MAP('X', 0x2f|SHIFT)

MAP('(', 0x30|SHIFT)
MAP(')', 0x31|SHIFT)
SPMAP('^', 0x34|SHIFT)      // cent
MAP('%', 0x35|SHIFT)
MAP('@', 0x36|SHIFT)
MAP('Z', 0x37|SHIFT)
MAP('$', 0x39|SHIFT)
MAP('*', 0x3c|SHIFT)
MAP('&', 0x3d|SHIFT)
MAP('#', 0x3e|SHIFT)
MAP('!', 0x3f|SHIFT)
