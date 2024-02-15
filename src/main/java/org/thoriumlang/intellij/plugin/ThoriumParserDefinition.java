/*
 * Copyright 2019 Christophe Pollet
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thoriumlang.intellij.plugin;

import com.intellij.lang.ASTNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import org.antlr.intellij.adaptor.lexer.PSIElementTypeFactory;
import org.antlr.intellij.adaptor.lexer.RuleIElementType;
import org.antlr.intellij.adaptor.lexer.TokenIElementType;
import org.antlr.intellij.adaptor.parser.ANTLRParserAdaptor;
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.tree.ParseTree;
import org.jetbrains.annotations.NotNull;
import org.thoriumlang.compiler.antlr.ThoriumLexer;
import org.thoriumlang.compiler.antlr.ThoriumParser;
import org.thoriumlang.intellij.plugin.antlr4.ANTLRLexerAdaptorFactory;
import org.thoriumlang.intellij.plugin.antlr4.ThoriumParserFactory;
import org.thoriumlang.intellij.plugin.psi.FileRoot;
import org.thoriumlang.intellij.plugin.psi.MethodDefinition;
import org.thoriumlang.intellij.plugin.psi.MethodSignature;
import org.thoriumlang.intellij.plugin.psi.TypeDef;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ThoriumParserDefinition implements ParserDefinition {
    public static final TokenIElementType LINE_COMMENT;

    public static final TokenIElementType IDENTIFIER;
    public static final TokenIElementType BLOCK_COMMENT;
    public static final TokenIElementType DOC_COMMENT;
    private static final List<TokenIElementType> TOKEN_ELEMENT_TYPES;

    private static final IFileElementType FILE = new IFileElementType(ThoriumLanguage.INSTANCE);

    static {
        PSIElementTypeFactory.defineLanguageIElementTypes(
                ThoriumLanguage.INSTANCE,
                IntStream.rangeClosed(0, ThoriumParser.VOCABULARY.getMaxTokenType())
                        .mapToObj(i -> {
                                    String name = ThoriumParser.VOCABULARY.getLiteralName(i);
                                    if (name != null) {
                                        return name;
                                    }
                                    name = ThoriumParser.VOCABULARY.getSymbolicName(i);
                                    if (name != null) {
                                        return name;
                                    }
                                    return "<INVALID>";
                                }
                        )
                        .collect(Collectors.toList())
                        .toArray(new String[]{}),
                ThoriumParser.ruleNames
        );

        TOKEN_ELEMENT_TYPES = PSIElementTypeFactory.getTokenIElementTypes(ThoriumLanguage.INSTANCE);

        IDENTIFIER = TOKEN_ELEMENT_TYPES.get(ThoriumLexer.IDENTIFIER);
        LINE_COMMENT = TOKEN_ELEMENT_TYPES.get(ThoriumLexer.LINE_COMMENT);
        BLOCK_COMMENT = TOKEN_ELEMENT_TYPES.get(ThoriumLexer.BLOCK_COMMENT);
        DOC_COMMENT = TOKEN_ELEMENT_TYPES.get(ThoriumLexer.DOC_COMMENT);
    }

    @NotNull
    @Override
    public Lexer createLexer(Project project) {
        return ANTLRLexerAdaptorFactory.newInstance();
    }

    @Override
    public PsiParser createParser(Project project) {
        return new ANTLRParserAdaptor(ThoriumLanguage.INSTANCE, ThoriumParserFactory.newInstance()) {
            @Override
            protected ParseTree parse(Parser parser, IElementType root) {
                return ((ThoriumParser) parser).root();
            }
        };
    }

    @NotNull
    @Override
    public TokenSet getWhitespaceTokens() {
        return PSIElementTypeFactory.createTokenSet(
                ThoriumLanguage.INSTANCE,
                ThoriumLexer.WS
        );
    }

    @NotNull
    @Override
    public TokenSet getCommentTokens() {
        return PSIElementTypeFactory.createTokenSet(
                ThoriumLanguage.INSTANCE,
                ThoriumLexer.LINE_COMMENT,
                ThoriumLexer.BLOCK_COMMENT,
                ThoriumLexer.DOC_COMMENT
        );
    }

    @NotNull
    @Override
    public TokenSet getStringLiteralElements() {
        return PSIElementTypeFactory.createTokenSet(
                ThoriumLanguage.INSTANCE,
                ThoriumLexer.STRING
        );
    }

    @Override
    public IFileElementType getFileNodeType() {
        return FILE;
    }

    @Override
    public PsiFile createFile(FileViewProvider viewProvider) {
        return new FileRoot(viewProvider);
    }

    @NotNull
    @Override
    public PsiElement createElement(ASTNode node) {
        IElementType elType = node.getElementType();

        if (elType instanceof TokenIElementType) {
            return new ANTLRPsiNode(node);
        }

        if (!(elType instanceof RuleIElementType)) {
            return new ANTLRPsiNode(node);
        }

        RuleIElementType ruleElType = (RuleIElementType) elType;

        switch (ruleElType.getRuleIndex()) {
            case ThoriumParser.RULE_typeDef:
                return new TypeDef(node, elType);
            case ThoriumParser.RULE_methodSignature:
                return new MethodSignature(node, elType);
            case ThoriumParser.RULE_methodDef:
                return new MethodDefinition(node, elType);
            default:
                return new ANTLRPsiNode(node);
        }
    }
}
