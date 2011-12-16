# $Id: Makefile,v 1.2 2011/12/16 14:21:37 drmiller Exp $

all:

waveforms: nDS7.png RD.png WR.png

nDS7.png: nDS7-nCL.BMP nDS7-nST.BMP nDS7-CPR-nCUP.BMP nDS7-HOLD.BMP
	./catscope -o nDS7 -c 130 \
			-t "nDS7 nCL nST CPR/nCUP hold" -r "110 75 85" \
			nDS7-nCL.BMP nDS7-nST.BMP nDS7-CPR-nCUP.BMP nDS7-HOLD.BMP

RD.png: nSTB-nWRITE-RD.BMP nSTB-nWAIT-RD.BMP nSTB-DATA-RD.BMP
	./catscope -o RD -c 125 -t "nSTB nWRITE nWAIT data" -r "110 75 90" \
			nSTB-nWRITE-RD.BMP nSTB-nWAIT-RD.BMP nSTB-DATA-RD.BMP

WR.png: nSTB-nWRITE-WR.BMP nSTB-nWAIT-WR.BMP
	./catscope -o WR -c 125 -t "nSTB nWRITE nWAIT" -r "110 75 90" \
			nSTB-nWRITE-WR.BMP nSTB-nWAIT-WR.BMP

