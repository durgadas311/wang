Name "Wang 600 Simulator"
OutFile "wang600apc.exe"
Section "Wang 600 Simulator"
	Var /GLOBAL WANG600HOME
	ReadEnvStr $0 HOMEDRIVE
	ReadEnvStr $1 HOMEPATH
	StrCpy $INSTDIR "$0$1\wang600apc"
	StrCpy $WANG600HOME "$0$1\Wang600Files"
	CreateDirectory $INSTDIR
	CreateDirectory $WANG600HOME
	SetOutPath $INSTDIR
	File "wang600.jar"
	File "Wang600.bat"
	File /r "icons\*.ico"
	SetOutPath $WANG600HOME
	File /r "progs\*.wng"
	CreateShortCut "$DESKTOP\Wang600.lnk" "$INSTDIR\Wang600.bat" "" \
		"$INSTDIR\wang600-48x48.ico" 0 SW_SHOWMINIMIZED
	WriteUninstaller $INSTDIR\uninstall.exe
SectionEnd
Section "Uninstall"
	Delete $DESKTOP\Wang600.lnk
	Delete $INSTDIR\*.jar
	Delete $INSTDIR\*.bat
	Delete $INSTDIR\*.ico
	Delete $INSTDIR\uninstall.exe
	RMDir $INSTDIR
SectionEnd
