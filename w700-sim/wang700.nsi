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
	File "Wang714.bat"
	File /r "icons\*.ico"
	File /r "..\common\icons\*.ico"
	CreateShortCut "$DESKTOP\Wang700.lnk" "$INSTDIR\Wang700.bat" "" \
		"$INSTDIR\wang700-48x48.ico" 0 SW_SHOWMINIMIZED
	CreateShortCut "$DESKTOP\Wang714.lnk" "$INSTDIR\Wang714.bat" "" \
		"$INSTDIR\WangX14Edit-128x128.ico" 0 SW_SHOWMINIMIZED
	SetOutPath $WANG700HOME
	File "progs\*.w7*"
	File "progs\*.txt*"
	File "progs\*.wfl*"
	WriteUninstaller $INSTDIR\uninstall.exe
	Exec 'java -cp $INSTDIR\wang700.jar w700initProps'
SectionEnd
Section "Uninstall"
	Delete $DESKTOP\Wang700.lnk
	Delete $DESKTOP\Wang714.lnk
	Delete $INSTDIR\*.jar
	Delete $INSTDIR\*.bat
	Delete $INSTDIR\*.ico
	Delete $INSTDIR\uninstall.exe
	RMDir $INSTDIR
SectionEnd
