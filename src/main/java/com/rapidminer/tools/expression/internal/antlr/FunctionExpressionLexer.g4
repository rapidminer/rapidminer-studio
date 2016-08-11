/*
 * Lexer Grammar for the FunctionExpressionLexer. Defines the tokens that are also used in the Parser Grammar.
 * 
 * @author Gisa Schaefer
 * @since 6.5.0
 */
lexer grammar FunctionExpressionLexer;

//operation tokens
PLUS: '+'; 
MINUS: '-';
MULTIPLY: '*';
DIVIDE: '/';
MODULO: '%';
POWER: '^';
LESS: '<';
LEQ: '<=';
GREATER: '>';
GEQ: '>=';
EQUALS: '==';
NOT_EQUALS: '!=';
NOT: '!';
OR: '||';
AND: '&&';


LPARENTHESIS: '(';
RPARENTHESIS: ')';

COMMA: ',';

/* function names, constants and attribute names outside [ ] must be alphanumeric */
NAME: ([A-Za-z])+([A-Za-z_0-9]*);

INTEGER: DIGITS;

/* real number, can be in scientific notation */
REAL : (DIGITS ('.' DIGITS)?| ('.' DIGITS)) EXPONENT?;
    
fragment
DIGITS: ('0'..'9')+;   

fragment
EXPONENT: ('e'|'E') ('+'|'-')? DIGITS;

/* [...] */
ATTRIBUTE: LSQUARE_BRACKET INSIDE_ATTRIBUTE RSQUARE_BRACKET;

/* "..." */
STRING: OPENING_QOUTES INSIDE_STRING CLOSING_QUOTES;

/* %{...} */
SCOPE_CONSTANT: SCOPE_OPEN INSIDE_SCOPE SCOPE_CLOSE;

/* #{...} */
INDIRECT_SCOPE_CONSTANT: INDIRECT_SCOPE_OPEN INSIDE_SCOPE SCOPE_CLOSE;

/* no newlines or tabulators allowed inside attribute or scope constant names, marker brackets need to be escaped */    
fragment
INSIDE_ATTRIBUTE: (~('\n'|'\t'|']'|'['|'\\')|'\\'(']'|'['|'\\'))+;

fragment
INSIDE_SCOPE: (~('\n'|'\t'|'}'|'{'|'\\')|'\\'('}'|'{'|'\\'))+;

/*inside a string no '"' or '\' are allowed, except inside '\n','\t', '\r', '\"' ,'\\' or unicode symbols */
fragment
INSIDE_STRING: (~('"'|'\\')|'\n'|'\t'|'\r'|'\\'('\\'|'"')|UNICODE)*;

fragment
UNICODE: '\\''u'UNICODE_CHAR UNICODE_CHAR UNICODE_CHAR UNICODE_CHAR;

fragment
UNICODE_CHAR: [A-Fa-f0-9];

/* change to another mode when encountering an attribute, a string or a scope constant */
LSQUARE_BRACKET : '[' -> mode(INSIDE_ATTRIBUTE);
OPENING_QOUTES : '"' -> mode(INSIDE_STRING);
SCOPE_OPEN: '%{' -> mode(INSIDE_SCOPE);
INDIRECT_SCOPE_OPEN: '#{' -> mode(INSIDE_SCOPE);

WHITESPACES : [ \t\r\n]+ -> skip ; /* skip spaces, tabs, newlines*/ 
/*end of DEFAULT_MODE */


/*don't skip spaces; go back to default mode when closing ']' appears */
mode INSIDE_SCOPE;
SCOPE_CLOSE: '}'  -> mode(DEFAULT_MODE);


/*don't skip spaces; go back to default mode when closing ']' appears */
mode INSIDE_ATTRIBUTE; 
RSQUARE_BRACKET : ']' -> mode(DEFAULT_MODE);


/* don't skip spaces, tabs and newlines; leave when closing '"' appears */
mode INSIDE_STRING; 
CLOSING_QUOTES: '"' ->mode(DEFAULT_MODE);
