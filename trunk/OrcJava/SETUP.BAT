@ECHO OFF
ASSOC .orc=OrcScript
FTYPE OrcScript=javaw -jar "%CD%\@ORCJAR@" "%%1"
