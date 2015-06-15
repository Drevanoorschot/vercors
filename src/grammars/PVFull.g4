grammar PVFull;

@header {
package pv.parser;
}

@lexer::members{
  public final static int COMMENT=ML_COMMENT;
  public final static int LINEDIRECTION=Integer.MAX_VALUE;
}

program  : (claz|kernel)* (block)? ;

claz : contract 'class' ID '{'( field | method | function | constructor | abs_decl )* '}' ;

kernel : 'kernel' ID '{' ( kernel_field | method | function )* '}' ;

kernel_field : ('global' | 'local') type ID ( ',' ID )* ';' ;

field : type ID ( ',' ID )* ';' ;

function : contract 'static'? type ID '(' args ')' '=' expr ';' ;

method : contract type ID '(' args ')' block ;

constructor : contract ID '(' args ')' block ;

abs_decl : contract type ID '(' args ')' ';' ;

contract :
 ( 'modifies' expr ';'
 | 'requires' expr ';'
 | 'ensures' expr ';'
 | 'given' type ID ';'
 | 'yields' type ID ';'
 )*;

args : type ID ( ',' type ID )* | ;

expr
 : lexpr
 | ID ':' expr
 | expr 'with' block 
 | expr 'then' block 
 | ID '<' type '>' values  
 | ('!'|'-') expr
 | expr '^^' expr
 | expr ('*'|'/'|'%') expr
 | expr ( '+' | '-' ) expr
 | expr ( '<' | '<=' | '>=' | '>') expr
 | expr ( '==' | '!=' ) expr
 | expr ('&&' | '**') expr
 | expr ('||' | '==>') expr
 | expr 'in' expr
 | expr '?' expr ':' expr
 | '?' ID
 | 'unfolding' expr 'in' expr 
 | lexpr '->' ID tuple
 | (lexpr | 'Value' | 'Perm' | 'PointsTo' | 'Hist' | '\\old' | '?' ) tuple
 | '(' ('\\sum' | '\\exists' | '\\forall' | '\\forall*') type ID ';' expr (';' expr )? ')'
 | '(' expr ')'
 | 'new' ID tuple
 | 'new' type '[' expr ']'
 | 'null'
 | 'true'
 | 'false'
 | '\\result'
 | ID
 | NUMBER
 | '[' expr ']' expr
 | '|' expr '|'
 | values
 ;

values : '{' ( | expr (',' expr)*) '}';

tuple : '(' ( | expr (',' expr)*) ')';

block : '{' statement* '}' ;

statement
 : 'return' expr ';'
 | 'lock' expr ';'
 | 'unlock' expr ';'
 | 'wait' expr ';'
 | 'notify' expr ';'
 | 'fork' expr ';'
 | 'join' expr ';'
 | 'fold' expr ';'
 | 'unfold' expr ';'
 | 'assert' expr ';' 
 | 'assume' expr ';' 
 | 'witness' expr ';' 
 | 'if' '(' expr ')' block ( 'else' block )?
 | 'barrier' '(' fence_list ')' ( '{' contract '}' | contract block )
 | 'for' '(' iters ')' with_then contract block
 | ( 'batch' | 'par' ) ID '(' iters ';' decls ';' expr ')' with_then contract block
 | 'atomic' '(' id_list ')' block 
 | invariant 'while' '(' expr ')' block
 | type ID ('=' expr | (',' ID)* ) ';'
 | expr ';'
 | block
 | lexpr '=' expr ';'
 | '{*' expr '*}'
 | 'action' expr ',' expr block 
 | 'create' expr ',' expr ';'
 | 'destroy' expr ',' expr ',' expr ';'
 | 'goto' ID ';'
 | 'label' ID ';'
 ;

id_list : ( ID ( ',' ID )* )? ;

with_then : ( 'with' block )? ('then' block)? ;

iters : ( iter ( ',' iter )* )? ;

iter : type ID '=' expr '..' expr ;

decls  : ( decl ( ',' decl )* )? ;

decl : type ID ( '=' expr )? ;

fence_list : ( 'local' | 'global' )* ;

invariant : ( 'loop_invariant' expr ';' )* ;

lexpr : ('this' | '\\result' | ID ) ('.' ID | '[' expr ']' )* ; 

type
 : ID '<' type '>'
 | ( 'process' | 'int' | 'boolean' | 'zfrac' | 'frac' | 'resource' | 'void' | ID | classType ) ('[' expr? ']')*
 ;

classType : ID typeArgs?;

typeArgs : '<' expr (',' expr)* '>';

ID  : ('a'..'z'|'A'..'Z') ('a'..'z'|'A'..'Z'|'0'..'9'|'_')*;
NUMBER : ('0'..'9')+;

ML_COMMENT : '/*' .*? '*/' -> channel(ML_COMMENT) ;
SL_COMMENT : '//' .*? '\n' -> channel(ML_COMMENT) ;

WS  :   (   ' '
        |   '\t'
        |   '\r'
        |   '\n'
        )+ -> skip ;

EmbeddedLatex
    : '#' ~[\r\n]* '#' -> skip
    ;
