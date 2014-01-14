// Copyright (c) 2013,2014 Douglas Miller
// $Id: Wang1200_Keyboard.java,v 1.4 2014/01/14 21:53:51 drmiller Exp $

abstract class Wang1200_Keyboard extends Wang_Keyboard
{
	static final long serialVersionUID = 311012000000000L;
	public abstract int getMode2(boolean clear); // mode bits, 0-3, a.k.a D3
	public abstract void setRECORD(boolean on);
	public abstract void setTAPE_MOV_L(boolean on);
	public abstract void setTAPE_MOV_R(boolean on);
	public abstract void setNO_ADJUST(boolean on);
	public abstract void setEND_DOC(boolean on);
	public abstract void setSKIP(boolean on);
	public abstract void setSEARCH(boolean on);
	public abstract void setCHAR_STOP(boolean on);
}
