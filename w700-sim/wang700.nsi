Name "Wang 700 Simulator"
RequestExecutionLevel user
OutFile "wang700apc.exe"
Section "Wang 700 Simulator"
	Var /GLOBAL WANG700HOME
	ExpandEnvStrings $INSTDIR "%HOMEDRIVE%%HOMEPATH%\wang700apc"
	ExpandEnvStrings $WANG700HOME "%HOMEDRIVE%%HOMEPATH%\Wang700Files"
	CreateDirectory $INSTDIR
	CreateDirectory $WANG700HOME
	SetOutPath $INSTDIR
	File "wang700.jar"
	File "Wang700.bat"
	File /r "icons\*.ico"
	CreateShortCut "$DESKTOP\Wang700.lnk" "$INSTDIR\Wang700.bat" "" \
		"$INSTDIR\wang700-48x48.ico" 0 SW_SHOWMINIMIZED
	SetOutPath $WANG700HOME
	File /r "progs\*.wng"
	WriteUninstaller $INSTDIR\uninstall.exe
SectionEnd
Section "Uninstall"
	Delete $DESKTOP\Wang700.lnk
	Delete $INSTDIR\*.jar
	Delete $INSTDIR\*.bat
	Delete $INSTDIR\*.ico
	Delete $INSTDIR\uninstall.exe
	RMDir $INSTDIR
SectionEnd
Function ".onInstSuccess"
	MessageBox MB_OK "This program requires environment vairables WANG700_HOST and WANG700_PORT be set up"
FunctionEnd
