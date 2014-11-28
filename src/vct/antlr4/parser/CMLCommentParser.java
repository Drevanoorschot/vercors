package vct.antlr4.parser;

import hre.HREError;

import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.tree.ParseTree;

import vct.col.ast.ASTNode;
import vct.col.ast.ASTSequence;
import vct.col.ast.Contract;
import vct.col.ast.ProgramUnit;
import vct.parsers.CMLLexer;
import vct.parsers.CMLParser;

/**
 * Parser for CML comments.
 * 
 */
public class CMLCommentParser extends CommentParser<CMLParser,CMLLexer> {

  public CMLCommentParser() {
    super(new CMLParser(null), new CMLLexer(null));
  }

  @Override
  public TempSequence parse_contract(ASTSequence<?> seq) {
    ParseTree tree=parser.specificationSequence(); //DRB --Changed
    return CMLtoCOL.convert(tree, "embedded_comments", tokens, parser);
  }

  @Override
  public TempSequence parse_annotations() {
	throw new HREError("annotations for C not defined yet.");
  }

}
