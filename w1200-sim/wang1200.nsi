Name "Wang 1200 Simulator"
RequestExecutionLevel user
OutFile "wang1200wps.exe"
Section "Wang 1200 Simulator"
	Var /GLOBAL WANG1200HOME
	ExpandEnvStrings $INSTDIR "%HOMEDRIVE%%HOMEPATH%\wang1200wps"
	ExpandEnvStrings $WANG1200HOME "%HOMEDRIVE%%HOMEPATH%\Wang1200Files"
	CreateDirectory $INSTDIR
	CreateDirectory $WANG1200HOME
	SetOutPath $INSTDIR
	File "wang1200.jar"
	File "Wang1200.bat"
	File /r "icons\*.ico"
	CreateShortCut "$DESKTOP\Wang1200.lnk" "$INSTDIR\Wang1200.bat" "" \
		"$INSTDIR\wang1200-48x48.ico" 0 SW_SHOWMINIMIZED
	SetOutPath $WANG1200HOME
	File /r "examples\*.wng"
	WriteUninstaller $INSTDIR\uninstall.exe
SectionEnd
Section "Uninstall"
	Delete $DESKTOP\Wang1200.lnk
	Delete $INSTDIR\*.jar
	Delete $INSTDIR\*.bat
	Delete $INSTDIR\*.ico
	Delete $INSTDIR\uninstall.exe
	RMDir $INSTDIR
SectionEnd
Function ".onInstSuccess"
	MessageBox MB_OK "This program requires environment vairables WANG1200_HOST and WANG1200_PORT be set up"
FunctionEnd
