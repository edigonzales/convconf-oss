grammar Lm;

@header { package guru.interlis.convconf.parser; }

lmFile: 'LM' ID ';' statement* EOF;

statement
    : valueMapDecl
    | dataDecl
    | inspectionDecl
    ;

valueMapDecl: 'VALUEMAP' ID '{' valueMapEntry+ '}';
valueMapEntry: INT '->' qname ';';

dataDecl: 'DATA' ID 'FROM' ID 'CLASS' qname '{' dataStmt* '}';
dataStmt
    : columnStmt
    | 'IDENT' ID ';'
    | 'WHERE' ID '=' STRING ';'
    | directionStmt
    | conversionStmt
    | aliasStmt
    | withStmt
    | annexeStmt
    | annexedStmt
    | joinStmt
    | nestingStmt
    ;

inspectionDecl: 'INSPECTION' 'ALL' ID 'FROM' ID 'CLASS' qname '{' inspectionStmt* '}';
inspectionStmt
    : columnStmt
    | 'IDENT' ID ';'
    | 'PARENT' ID ';'
    | 'STRUCTATTR' ID 'USING' ID ';'
    | 'CLASSCOL' ID 'USING' ID ';'
    | directionStmt
    | conversionStmt
    | aliasStmt
    | withStmt
    ;

columnStmt: 'COLUMN' ID '->' targetPath ('USING' ID)? ';';
directionStmt: 'DIRECTION' ('<->' | '<-' | '->') ';';
conversionStmt: 'CONVERSION' ID '(' ID '--' ID ')' ';';
aliasStmt: 'ALIAS' ID '~' qname ';';
withStmt: 'WITH' ID '{' columnStmt+ '}';
annexeStmt: 'ANNEXE' qname ';';
annexedStmt: 'ANNEXED' ID ';';
joinStmt: 'JOIN' ('INNER' | 'LEFT' | 'RIGHT') ID 'ON' ID '=' ID ';';
nestingStmt: 'NESTING' ID 'BY' ID ';';

targetPath: DOLLAR_ID | ID ('.' ID)*;
qname: ID ('.' ID)*;

DOLLAR_ID: '$CLASS' | '$PARENT' | '$STRUCTATTR' | '$IDENT';
ID: [a-zA-Z_][a-zA-Z0-9_]*;
INT: [0-9]+;
STRING: '\'' (~['\\] | '\\' .)* '\'';
WS: [ \t\r\n]+ -> skip;
COMMENT: '#' ~[\r\n]* -> skip;
