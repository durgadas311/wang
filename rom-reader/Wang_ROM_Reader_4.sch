EESchema Schematic File Version 1
LIBS:power,./wang,device,conn,linear,regul,74xx,cmos4000,adc-dac,memory,xilinx,special,microcontrollers,dsp,microchip,analog_switches,motorola,texas,intel,audio,interface,digital-audio,philips,display,cypress,siliconi,contrib,valves,./Wang_ROM_Reader.cache
EELAYER 23  0
EELAYER END
$Descr A4 11700 8267
Sheet 2 4
Title "Wang Wire-weave ROM Reader"
Date "30 nov 2011"
Rev ""
Comp ""
Comment1 ""
Comment2 ""
Comment3 ""
Comment4 ""
$EndDescr
Text Label 6250 6000 0    60   ~
ROM Bus
Connection ~ 6100 1050
Wire Wire Line
	6100 950  6100 1050
Connection ~ 5900 1350
Wire Wire Line
	5900 1250 5900 1350
Connection ~ 6300 1250
Wire Wire Line
	6450 1250 6300 1250
Wire Wire Line
	3800 3350 2250 3350
Wire Wire Line
	3800 3150 2250 3150
Wire Wire Line
	3800 2950 2250 2950
Wire Wire Line
	3800 2750 2250 2750
Wire Wire Line
	3800 2550 2250 2550
Wire Wire Line
	3800 2350 2250 2350
Wire Wire Line
	3800 1950 2250 1950
Wire Wire Line
	3800 3700 2250 3700
Wire Wire Line
	3800 3900 2250 3900
Wire Wire Line
	3800 4100 2250 4100
Wire Wire Line
	3800 4300 2250 4300
Wire Wire Line
	3800 4500 2250 4500
Wire Wire Line
	3800 4700 2250 4700
Wire Wire Line
	3800 4900 2250 4900
Wire Wire Line
	4650 5000 5850 5000
Wire Wire Line
	4650 4800 5850 4800
Wire Wire Line
	4650 4600 5850 4600
Wire Wire Line
	4650 4400 5850 4400
Wire Wire Line
	4650 4200 5850 4200
Wire Wire Line
	4650 4000 5850 4000
Wire Wire Line
	4650 3800 5850 3800
Wire Wire Line
	4650 3600 5850 3600
Wire Wire Line
	4650 3250 5850 3250
Wire Wire Line
	4650 3050 5850 3050
Wire Wire Line
	4650 2850 5850 2850
Wire Wire Line
	4650 2650 5850 2650
Wire Wire Line
	4650 2450 5850 2450
Wire Wire Line
	4650 2250 5850 2250
Wire Wire Line
	4650 1950 5850 1950
Wire Bus Line
	5850 1850 5850 6000
Wire Wire Line
	3800 2050 3650 2050
Wire Wire Line
	4650 2050 4800 2050
Wire Wire Line
	3800 2150 3650 2150
Wire Wire Line
	3650 2150 3650 3450
Wire Bus Line
	2250 1850 2250 6000
Wire Bus Line
	2250 6000 6600 6000
Wire Wire Line
	4650 2150 5850 2150
Wire Wire Line
	4650 2350 5850 2350
Wire Wire Line
	4650 2550 5850 2550
Wire Wire Line
	4650 2750 5850 2750
Wire Wire Line
	4650 2950 5850 2950
Wire Wire Line
	4650 3150 5850 3150
Wire Wire Line
	4650 3350 5850 3350
Wire Wire Line
	4650 3700 5850 3700
Wire Wire Line
	4650 3900 5850 3900
Wire Wire Line
	4650 4100 5850 4100
Wire Wire Line
	4650 4300 5850 4300
Wire Wire Line
	4650 4500 5850 4500
Wire Wire Line
	4650 4700 5850 4700
Wire Wire Line
	4650 4900 5850 4900
Wire Wire Line
	3800 5000 2250 5000
Wire Wire Line
	3800 4800 2250 4800
Wire Wire Line
	3800 4600 2250 4600
Wire Wire Line
	3800 4400 2250 4400
Wire Wire Line
	3800 4200 2250 4200
Wire Wire Line
	3800 4000 2250 4000
Wire Wire Line
	3800 3800 2250 3800
Wire Wire Line
	3800 3600 2250 3600
Wire Wire Line
	3800 2250 2250 2250
Wire Wire Line
	3800 2450 2250 2450
Wire Wire Line
	3800 2650 2250 2650
Wire Wire Line
	3800 2850 2250 2850
Wire Wire Line
	3800 3050 2250 3050
Wire Wire Line
	3800 3250 2250 3250
Wire Wire Line
	6450 1150 6300 1150
Wire Wire Line
	6300 1150 6300 1550
Wire Wire Line
	6450 1350 3650 1350
Wire Wire Line
	3650 1350 3650 2050
Wire Wire Line
	6450 1050 4800 1050
Wire Wire Line
	4800 1050 4800 2050
$Comp
L GND #PWR05
U 1 1 4ED69CD4
P 6300 1550
F 0 "#PWR05" H 6300 1550 30  0001 C C
F 1 "GND" H 6300 1480 30  0001 C C
	1    6300 1550
	1    0    0    -1  
$EndComp
$Comp
L CONN_4 P3
U 1 1 4ED69C62
P 6800 1200
F 0 "P3" V 6750 1200 50  0000 C C
F 1 "CONN_4" V 6850 1200 50  0000 C C
	1    6800 1200
	1    0    0    -1  
$EndComp
Text Label 2800 1950 0    60   ~
jad0
Text Label 2800 2250 0    60   ~
CPR
Text Label 2800 2350 0    60   ~
njad8
Text Label 2800 2450 0    60   ~
njad7
Text Label 2800 2550 0    60   ~
jad4
Text Label 2800 2650 0    60   ~
jad5
Text Label 2800 2750 0    60   ~
jad6
Text Label 2800 2850 0    60   ~
nMOP1
Text Label 2800 2950 0    60   ~
nMOP2
Text Label 2800 3050 0    60   ~
nMOP3
Text Label 2800 3150 0    60   ~
nBd
Text Label 2800 3250 0    60   ~
nBC0
Text Label 2800 3350 0    60   ~
nBC1
Text Label 2800 5000 0    60   ~
nJL1
Text Label 2800 4900 0    60   ~
nJL0
Text Label 2800 4800 0    60   ~
(spare)
Text Label 2800 4700 0    60   ~
jad3
Text Label 2800 4600 0    60   ~
jad2
Text Label 2800 4500 0    60   ~
jad1
Text Label 2800 4400 0    60   ~
jl
Text Label 2800 4300 0    60   ~
nMOP0
Text Label 2800 4200 0    60   ~
nKK3
Text Label 2800 4100 0    60   ~
nKK2
Text Label 2800 4000 0    60   ~
nKK1
Text Label 2800 3900 0    60   ~
nKK0
Text Label 2800 3800 0    60   ~
S.P.GND
Text Label 2800 3600 0    60   ~
nST2
Text Label 2800 3700 0    60   ~
nST3
Text Label 5150 5000 0    60   ~
nJL2
Text Label 5150 4900 0    60   ~
nJH0
Text Label 5150 4800 0    60   ~
nJH1
Text Label 5150 4700 0    60   ~
nJH2
Text Label 5150 4600 0    60   ~
nJAD0
Text Label 5150 4500 0    60   ~
nJAD1
Text Label 5150 4400 0    60   ~
nJAD2
Text Label 5150 4300 0    60   ~
nJAD3
Text Label 5150 4200 0    60   ~
nJAD4
Text Label 5150 4100 0    60   ~
nJAD5
Text Label 5150 4000 0    60   ~
nJAD6
Text Label 5150 3900 0    60   ~
nJAD7
Text Label 5150 3800 0    60   ~
nJAD8
Text Label 5150 3700 0    60   ~
nST0
Text Label 5150 3600 0    60   ~
nST1
Text Label 5150 3350 0    60   ~
nAC
Text Label 5150 3250 0    60   ~
nAOP0
Text Label 5150 3150 0    60   ~
nAOP1
Text Label 5150 3050 0    60   ~
nAOP2
Text Label 5150 2950 0    60   ~
nZO0
Text Label 5150 2850 0    60   ~
nZO1
Text Label 5150 2750 0    60   ~
nZO2
Text Label 5150 2650 0    60   ~
nBI0
Text Label 5150 2550 0    60   ~
nBI1
Text Label 5150 2450 0    60   ~
nBI2
Text Label 5150 2350 0    60   ~
nAI0
Text Label 5150 2250 0    60   ~
nAI1
Text Label 5150 2150 0    60   ~
nAI2
Text Label 5150 1950 0    60   ~
jh
$Comp
L +5V #PWR06
U 1 1 4ED69577
P 5900 1250
F 0 "#PWR06" H 5900 1340 20  0001 C C
F 1 "+5V" H 5900 1340 30  0000 C C
	1    5900 1250
	1    0    0    -1  
$EndComp
$Comp
L GND #PWR07
U 1 1 4ED69576
P 3650 3450
F 0 "#PWR07" H 3650 3450 30  0001 C C
F 1 "GND" H 3650 3380 30  0001 C C
	1    3650 3450
	1    0    0    -1  
$EndComp
$Comp
L +12V #PWR08
U 1 1 4ED69575
P 6100 950
F 0 "#PWR08" H 6100 900 20  0001 C C
F 1 "+12V" H 6100 1050 30  0000 C C
	1    6100 950 
	1    0    0    -1  
$EndComp
$Comp
L CONN_15X2 P2
U 1 1 4ED69574
P 4200 4250
F 0 "P2" V 4170 4250 60  0000 C C
F 1 "CONN_15X2" V 4280 4250 60  0000 C C
	1    4200 4250
	1    0    0    -1  
$EndComp
$Comp
L CONN_15X2 P1
U 1 1 4ED69573
P 4200 2600
F 0 "P1" V 4170 2600 60  0000 C C
F 1 "CONN_15X2" V 4280 2600 60  0000 C C
	1    4200 2600
	1    0    0    -1  
$EndComp
$EndSCHEMATC
