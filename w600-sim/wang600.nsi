Name "Wang 600 Simulator"
RequestExecutionLevel user
OutFile "wang600apc.exe"
Section "Wang 600 Simulator"
	Var /GLOBAL WANG600HOME
	ExpandEnvStrings $INSTDIR "%HOMEDRIVE%%HOMEPATH%\wang600apc"
	ExpandEnvStrings $WANG600HOME "%HOMEDRIVE%%HOMEPATH%\Wang600Files"
	CreateDirectory $INSTDIR
	CreateDirectory $WANG600HOME
	SetOutPath $INSTDIR
	File "wang600.jar"
	File "Wang600.bat"
	File /r "icons\*.ico"
	CreateShortCut "$DESKTOP\Wang600.lnk" "$INSTDIR\Wang600.bat" "" \
		"$INSTDIR\wang600-48x48.ico" 0 SW_SHOWMINIMIZED
	SetOutPath $WANG600HOME
	File /r "progs\*.w6*"
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
Function ".onInstSuccess"
	MessageBox MB_OK "This program requires environment vairables WANG600_HOST and WANG600_PORT be set up"
FunctionEnd
