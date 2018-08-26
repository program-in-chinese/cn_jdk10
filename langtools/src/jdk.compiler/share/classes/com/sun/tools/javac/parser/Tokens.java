/*
 * Copyright (c) 1999, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.sun.tools.javac.parser;

import java.util.Locale;

import com.sun.tools.javac.api.Formattable;
import com.sun.tools.javac.api.Messages;
import com.sun.tools.javac.parser.Tokens.Token.Tag;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Filter;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Names;

/** A class that defines codes/utilities for Java source tokens
 *  returned from lexical analysis.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class Tokens {

    private final Names names;

    /**
     * Keyword array. Maps name indices to Token.
     */
    private final TokenKind[] key;

    /**  The number of the last entered keyword.
     */
    private int maxKey = 0;

    /** The names of all tokens.
     */
    private Name[] tokenName = new Name[2 * TokenKind.values().length];

    public static final Context.Key<Tokens> tokensKey = new Context.Key<>();

    public static Tokens instance(Context context) {
        Tokens instance = context.get(tokensKey);
        if (instance == null)
            instance = new Tokens(context);
        return instance;
    }

    protected Tokens(Context context) {
        context.put(tokensKey, this);
        names = Names.instance(context);
        for (TokenKind t : TokenKind.values()) {
            if (t.name != null) {
                Name n = names.fromString(t.name);
                tokenName[2 * t.ordinal()] = n;
                if (n.getIndex() > maxKey) maxKey = n.getIndex();
                if (t.name_cn != null) {
                    Name n_cn = names.fromString(t.name_cn);
                    tokenName[2 * t.ordinal() + 1] = n_cn;
                    if (n_cn.getIndex() > maxKey) maxKey = n_cn.getIndex();
                }
                else {
                    tokenName[2 * t.ordinal() + 1] = null;
                }
            }
                //enterKeyword(t.name, t);
            else {
                tokenName[2 * t.ordinal()] = null;
                tokenName[2 * t.ordinal() + 1] = null;
            }
        }

        key = new TokenKind[maxKey+1];
        for (int i = 0; i <= maxKey; i++) key[i] = TokenKind.IDENTIFIER;
        for (TokenKind t : TokenKind.values()) {
            if (t.name != null) {
                key[tokenName[2 * t.ordinal()].getIndex()] = t;
                if (t.name_cn != null) {
                    key[tokenName[2 * t.ordinal() + 1].getIndex()] = t;
                }
            }
        }
    }

    //private void enterKeyword(String s, TokenKind token) {
        //Name n = names.fromString(s);
        //tokenName[token.ordinal()] = n;
        //if (n.getIndex() > maxKey) maxKey = n.getIndex();
    //}

    /**
     * Create a new token given a name; if the name corresponds to a token name,
     * a new token of the corresponding kind is returned; otherwise, an
     * identifier token is returned.
     */
    TokenKind lookupKind(Name name) {
        return (name.getIndex() > maxKey) ? TokenKind.IDENTIFIER : key[name.getIndex()];
    }

    TokenKind lookupKind(String name) {
        return lookupKind(names.fromString(name));
    }

    /**
     * This enum defines all tokens used by the javac scanner. A token is
     * optionally associated with a name.
     */
    public enum TokenKind implements Formattable, Filter<TokenKind> {
        EOF(),
        ERROR(),
        IDENTIFIER(Tag.NAMED),
        ABSTRACT("abstract", "象"),
        ASSERT("assert", "断", Tag.NAMED),
        BOOLEAN("boolean", "不二", Tag.NAMED),
        BREAK("break", "破"),
        BYTE("byte", "字", Tag.NAMED),
        CASE("case", "例"),
        CATCH("catch", "捕"),
        CHAR("char", "符", Tag.NAMED),
        CLASS("class", "类"),
        CONST("const", "常"),
        CONTINUE("continue", "继"),
        DEFAULT("default", "默"),
        DO("do", "运"),
        DOUBLE("double", "双", Tag.NAMED),
        ELSE("else", "另"),
        ENUM("enum", "举", Tag.NAMED),
        EXTENDS("extends", "承"),
        FINAL("final", "终"),
        FINALLY("finally", "末"),
        FLOAT("float", "浮", Tag.NAMED),
        FOR("for", "为"),
        GOTO("goto", "去"),
        IF("if", "如"),
        IMPLEMENTS("implements", "成"),
        IMPORT("import", "进"),
        INSTANCEOF("instanceof", "是"),
        INT("int", "整", Tag.NAMED),
        INTERFACE("interface", "接"),
        LONG("long", "长", Tag.NAMED),
        NATIVE("native", "原"),
        NEW("new", "新"),
        PACKAGE("package", "包"),
        PRIVATE("private", "私"),
        PROTECTED("protected", "保"),
        PUBLIC("public", "公"),
        RETURN("return", "返"),
        SHORT("short", "短", Tag.NAMED),
        STATIC("static", "固"),
        STRICTFP("strictfp", "严"),
        SUPER("super", "超", Tag.NAMED),
        SWITCH("switch", "分"),
        SYNCHRONIZED("synchronized", "同"),
        THIS("this", "此", Tag.NAMED),
        THROW("throw", "抛"),
        THROWS("throws", "弃"),
        TRANSIENT("transient", "暂"),
        TRY("try", "试"),
        VOID("void", "空", Tag.NAMED),
        VOLATILE("volatile", "易"),
        WHILE("while", "当"),
        INTLITERAL(Tag.NUMERIC),
        LONGLITERAL(Tag.NUMERIC),
        FLOATLITERAL(Tag.NUMERIC),
        DOUBLELITERAL(Tag.NUMERIC),
        CHARLITERAL(Tag.NUMERIC),
        STRINGLITERAL(Tag.STRING),
        TRUE("true", "真", Tag.NAMED),
        FALSE("false", "假", Tag.NAMED),
        NULL("null", "无", Tag.NAMED),
        UNDERSCORE("_", Tag.NAMED),
        ARROW("->"),
        COLCOL("::"),
        LPAREN("("),
        RPAREN(")"),
        LBRACE("{"),
        RBRACE("}"),
        LBRACKET("["),
        RBRACKET("]"),
        SEMI(";"),
        COMMA(","),
        DOT("."),
        ELLIPSIS("..."),
        EQ("="),
        GT(">"),
        LT("<"),
        BANG("!"),
        TILDE("~"),
        QUES("?"),
        COLON(":"),
        EQEQ("=="),
        LTEQ("<="),
        GTEQ(">="),
        BANGEQ("!="),
        AMPAMP("&&"),
        BARBAR("||"),
        PLUSPLUS("++"),
        SUBSUB("--"),
        PLUS("+"),
        SUB("-"),
        STAR("*"),
        SLASH("/"),
        AMP("&"),
        BAR("|"),
        CARET("^"),
        PERCENT("%"),
        LTLT("<<"),
        GTGT(">>"),
        GTGTGT(">>>"),
        PLUSEQ("+="),
        SUBEQ("-="),
        STAREQ("*="),
        SLASHEQ("/="),
        AMPEQ("&="),
        BAREQ("|="),
        CARETEQ("^="),
        PERCENTEQ("%="),
        LTLTEQ("<<="),
        GTGTEQ(">>="),
        GTGTGTEQ(">>>="),
        MONKEYS_AT("@"),
        CUSTOM;

        public final String name;
        public final String name_cn;
        public final Tag tag;

        TokenKind() {
            //this(null, Tag.DEFAULT);
            this(null, null, Tag.DEFAULT);
        }

        TokenKind(String name) {
            //this(name, Tag.DEFAULT);
            this(name, null, Tag.DEFAULT);
        }

        TokenKind(Tag tag) {
            //this(null, tag);
            this(null, null, tag);
        }

        TokenKind(String name, Tag tag) {
            //this.name = name;
            //this.tag = tag;
            this(name, null, tag);
        }

        TokenKind(String name, String name_cn) {
            //this.name = name;
            //this.tag = tag;
            this(name, name_cn, Tag.DEFAULT);
        }

        TokenKind(String name, String name_cn, Tag tag) {
            this.name = name;
            this.name_cn = name_cn;
            this.tag = tag;
        }

        public String toString() {
            switch (this) {
            case IDENTIFIER:
                return "token.identifier";
            case CHARLITERAL:
                return "token.character";
            case STRINGLITERAL:
                return "token.string";
            case INTLITERAL:
                return "token.integer";
            case LONGLITERAL:
                return "token.long-integer";
            case FLOATLITERAL:
                return "token.float";
            case DOUBLELITERAL:
                return "token.double";
            case ERROR:
                return "token.bad-symbol";
            case EOF:
                return "token.end-of-input";
            case DOT: case COMMA: case SEMI: case LPAREN: case RPAREN:
            case LBRACKET: case RBRACKET: case LBRACE: case RBRACE:
                return "'" + name + "'";
            default:
                return name;
            }
        }

        public String getKind() {
            return "Token";
        }

        public String toString(Locale locale, Messages messages) {
            return name != null ? toString() : messages.getLocalizedString(locale, "compiler.misc." + toString());
        }

        @Override
        public boolean accepts(TokenKind that) {
            return this == that;
        }
    }

    public interface Comment {

        enum CommentStyle {
            LINE,
            BLOCK,
            JAVADOC,
        }

        String getText();
        int getSourcePos(int index);
        CommentStyle getStyle();
        boolean isDeprecated();
    }

    /**
     * This is the class representing a javac token. Each token has several fields
     * that are set by the javac lexer (i.e. start/end position, string value, etc).
     */
    public static class Token {

        /** tags constants **/
        enum Tag {
            DEFAULT,
            NAMED,
            STRING,
            NUMERIC
        }

        /** The token kind */
        public final TokenKind kind;

        /** The start position of this token */
        public final int pos;

        /** The end position of this token */
        public final int endPos;

        /** Comment reader associated with this token */
        public final List<Comment> comments;

        Token(TokenKind kind, int pos, int endPos, List<Comment> comments) {
            this.kind = kind;
            this.pos = pos;
            this.endPos = endPos;
            this.comments = comments;
            checkKind();
        }

        Token[] split(Tokens tokens) {
            if (kind.name.length() < 2 || kind.tag != Tag.DEFAULT) {
                throw new AssertionError("Cant split" + kind);
            }

            TokenKind t1 = tokens.lookupKind(kind.name.substring(0, 1));
            TokenKind t2 = tokens.lookupKind(kind.name.substring(1));

            if (t1 == null || t2 == null) {
                throw new AssertionError("Cant split - bad subtokens");
            }
            return new Token[] {
                new Token(t1, pos, pos + t1.name.length(), comments),
                new Token(t2, pos + t1.name.length(), endPos, null)
            };
        }

        protected void checkKind() {
            if (kind.tag != Tag.DEFAULT) {
                throw new AssertionError("Bad token kind - expected " + Tag.STRING);
            }
        }

        public Name name() {
            throw new UnsupportedOperationException();
        }

        public String stringVal() {
            throw new UnsupportedOperationException();
        }

        public int radix() {
            throw new UnsupportedOperationException();
        }

        /**
         * Preserve classic semantics - if multiple javadocs are found on the token
         * the last one is returned
         */
        public Comment comment(Comment.CommentStyle style) {
            List<Comment> comments = getComments(Comment.CommentStyle.JAVADOC);
            return comments.isEmpty() ?
                    null :
                    comments.head;
        }

        /**
         * Preserve classic semantics - deprecated should be set if at least one
         * javadoc comment attached to this token contains the '@deprecated' string
         */
        public boolean deprecatedFlag() {
            for (Comment c : getComments(Comment.CommentStyle.JAVADOC)) {
                if (c.isDeprecated()) {
                    return true;
                }
            }
            return false;
        }

        private List<Comment> getComments(Comment.CommentStyle style) {
            if (comments == null) {
                return List.nil();
            } else {
                ListBuffer<Comment> buf = new ListBuffer<>();
                for (Comment c : comments) {
                    if (c.getStyle() == style) {
                        buf.add(c);
                    }
                }
                return buf.toList();
            }
        }
    }

    final static class NamedToken extends Token {
        /** The name of this token */
        public final Name name;

        public NamedToken(TokenKind kind, int pos, int endPos, Name name, List<Comment> comments) {
            super(kind, pos, endPos, comments);
            this.name = name;
        }

        protected void checkKind() {
            if (kind.tag != Tag.NAMED) {
                throw new AssertionError("Bad token kind - expected " + Tag.NAMED);
            }
        }

        @Override
        public Name name() {
            return name;
        }
    }

    static class StringToken extends Token {
        /** The string value of this token */
        public final String stringVal;

        public StringToken(TokenKind kind, int pos, int endPos, String stringVal, List<Comment> comments) {
            super(kind, pos, endPos, comments);
            this.stringVal = stringVal;
        }

        protected void checkKind() {
            if (kind.tag != Tag.STRING) {
                throw new AssertionError("Bad token kind - expected " + Tag.STRING);
            }
        }

        @Override
        public String stringVal() {
            return stringVal;
        }
    }

    final static class NumericToken extends StringToken {
        /** The 'radix' value of this token */
        public final int radix;

        public NumericToken(TokenKind kind, int pos, int endPos, String stringVal, int radix, List<Comment> comments) {
            super(kind, pos, endPos, stringVal, comments);
            this.radix = radix;
        }

        protected void checkKind() {
            if (kind.tag != Tag.NUMERIC) {
                throw new AssertionError("Bad token kind - expected " + Tag.NUMERIC);
            }
        }

        @Override
        public int radix() {
            return radix;
        }
    }

    public static final Token DUMMY =
                new Token(TokenKind.ERROR, 0, 0, null);
}
