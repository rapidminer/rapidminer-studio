/*
 * Parser Grammar for the FunctionExpressionParser. Relies on the tokens of the FunctionExpressionLexer
 * and defines the hierarchy of operations.
 * 
 * @author Gisa Schaefer
 * @since 6.5.0
 */
parser grammar FunctionExpressionParser;

options {   tokenVocab = FunctionExpressionLexer; }


/* operationExps ordered by precedence of operations */
operationExp
		: 
		op=NOT operationExp
		|op=(PLUS|MINUS) operationExp		
		|<assoc=right> operationExp op=POWER operationExp
		|operationExp op=(MULTIPLY|DIVIDE|MODULO) operationExp
		|operationExp op=(PLUS|MINUS) operationExp
		|operationExp op=(LESS|LEQ|GREATER|GEQ) operationExp
		|operationExp op=(EQUALS|NOT_EQUALS) operationExp
		|operationExp op=AND operationExp
		|operationExp op=OR operationExp
		|atomExp
		;
    
    
atomExp
   :     function | attribute | scopeConstant | indirectScopeConstant | string |variable |real |integer
   |	lowerExp
    ;
    
lowerExp:  LPARENTHESIS operationExp RPARENTHESIS;

/*a function can have any number of parameters, separated by comma */
function: NAME LPARENTHESIS (|operationExp (COMMA operationExp)*)RPARENTHESIS;

attribute: ATTRIBUTE;

scopeConstant: SCOPE_CONSTANT;

indirectScopeConstant: INDIRECT_SCOPE_CONSTANT;

string: STRING;

variable: NAME;

real: REAL;

integer: INTEGER;