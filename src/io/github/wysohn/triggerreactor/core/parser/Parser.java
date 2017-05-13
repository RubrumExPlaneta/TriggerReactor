/*******************************************************************************
 *     Copyright (C) 2017 wysohn
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package io.github.wysohn.triggerreactor.core.parser;

import java.awt.BorderLayout;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;

import io.github.wysohn.triggerreactor.core.Token;
import io.github.wysohn.triggerreactor.core.Token.Type;
import io.github.wysohn.triggerreactor.core.lexer.Lexer;
import io.github.wysohn.triggerreactor.core.lexer.LexerException;

public class Parser {
    private final Lexer lexer;

    private Token token;

    public Parser(Lexer lexer) throws IOException, LexerException {
        this.lexer = lexer;

        nextToken();
    }

    private void nextToken() throws IOException, LexerException{
        token = lexer.getToken();
    }

    private void skipEndLines() throws IOException, LexerException{
        while(token != null && token.type == Type.ENDL)
            nextToken();
    }

    public Node parse() throws IOException, LexerException, ParserException{
        Node root = new Node(new Token(Type.ROOT, "<ROOT>"));
        Node statement = null;
        while((statement = parseStatement()) != null)
            root.getChildren().add(statement);
        return root;
    }

    private Node parseStatement() throws ParserException, IOException, LexerException{
        skipEndLines();
        if(token != null){
            if("IF".equals(token.value)){
                Node ifNode = new Node(token);
                nextToken();

                //condition
                Node condition = parseLogic();
                if(condition == null)
                    throw new ParserException("Could not find condition for IF statement!");
                ifNode.getChildren().add(condition);

                //if body
                Node trueBody = new Node(new Token(Type.BODY, "<BODY>"));

                Node codes = null;
                while((codes = parseStatement()) != null
                        && !"ENDIF".equals(codes.getToken().value)
                        && !"ELSE".equals(codes.getToken().value)){
                    trueBody.getChildren().add(codes);
                }
                if(codes == null)
                    throw new ParserException("Could not find ENDIF statement!");
                ifNode.getChildren().add(trueBody);

                //else body
                if("ELSE".equals(codes.getToken().value)){
                    Node falseBody = new Node(new Token(Type.BODY, "<BODY>"));

                    while((codes = parseStatement()) != null
                            && !"ENDIF".equals(codes.getToken().value)){
                        falseBody.getChildren().add(codes);
                    }
                    if(codes == null)
                        throw new ParserException("Could not find ENDIF statement!");
                    ifNode.getChildren().add(falseBody);
                }

                //return
                return ifNode;
            } else if("ELSE".equals(token.value)) {
                Node elseNode = new Node(token);
                nextToken();
                return elseNode;
            } else if("ENDIF".equals(token.value)){
                Node endIfNode = new Node(token);
                nextToken();
                return endIfNode;
            }
            else if("WHILE".equals(token.value)){
                Node whileNode = new Node(token);
                nextToken();

                Node condition = parseComparison();
                if(condition == null)
                    throw new ParserException("Could not find condition for WHILE statement!");
                whileNode.getChildren().add(condition);

                Node body = new Node(new Token(Type.BODY, "<BODY>"));
                Node codes = null;
                while((codes = parseStatement()) != null && !"ENDWHILE".equals(codes.getToken().value)){
                    body.getChildren().add(codes);
                }
                if(codes == null)
                    throw new ParserException("Could not find ENDWHILE statement!");
                whileNode.getChildren().add(body);

                return whileNode;
            }
            else if("ENDWHILE".equals(token.value)){
                Node endWhileNode = new Node(token);
                nextToken();
                return endWhileNode;
            }
            else if(token.type == Type.ID){
                if(((String) token.value).charAt(0) == '#'){
                    String command = ((String) token.value).substring(1);
                    Node commandNode = new Node(new Token(Type.COMMAND, command));
                    nextToken();

                    List<Node> args = new ArrayList<>();
                    while(token != null && token.type != Type.ENDL){
                        Node node = parseComparison();
                        if(node != null)
                            args.add(node);
                    }
                    commandNode.getChildren().addAll(args);
                    nextToken();

                    return commandNode;
                }else{
                    Node left = parseId();
                    if(left == null)
                        throw new ParserException("Expected an Id but found nothing");

                    if(!"=".equals(token.value))
                        throw new ParserException("Expected '=' but found "+token);
                    Node assign = new Node(new Token(Type.OPERATOR, "="));
                    nextToken();

                    Node right = parseLogic();
                    if(right == null)
                        throw new ParserException("Expected logic but found nothing");

                    assign.getChildren().add(left);
                    assign.getChildren().add(right);

                    if(token.type != Type.ENDL)
                        throw new ParserException("Expected end of line but found "+token);
                    nextToken();

                    return assign;
                }
            }else{
                throw new ParserException("Unexpected token "+token);
            }
        }else{
            return null;
        }
    }
/*
    private Node parseAssignment() throws IOException, LexerException, ParserException{
        Node id = parseFactor();
        if(id == null)
            throw new ParserException("Expected Id but found nothing. Token: "+token);

        Node parent = parseAssignmentAndId(id);
        if(parent != null){
            return parent;
        } else {
            return id;
        }
    }

    private Node parseAssignmentAndId(Node left) throws IOException, LexerException, ParserException{
        if(token != null && "=".equals(token.value)){
            Node node = new Node(new Token(Type.ASSIGNMENT, token.value));
            nextToken();

            node.getChildren().add(left);

            Node logic = parseLogic();
            if(logic != null){
                node.getChildren().add(logic);
            }else{
                throw new ParserException("Expected a logic after ["+node.getToken().value+"] but found ["+token+"] !");
            }

            Node assignmentAndLogic = parseAssignmentAndId(node);
            if(assignmentAndLogic != null){
                return assignmentAndLogic;
            }else{
                return node;
            }
        }else{
            throw new ParserException("Unexpected token "+token);
        }
    }*/

    private Node parseLogic() throws IOException, LexerException, ParserException{
        Node comparison = parseComparison();

        Node parent = parseLogicAndComparison(comparison);
        if(parent != null){
            return parent;
        } else{
            return comparison;
        }
    }

    private Node parseLogicAndComparison(Node left) throws IOException, LexerException, ParserException{
        if(token != null && token.type == Type.OPERATOR_L
                &&("||".equals(token.value) || "&&".equals(token.value))){
            Node node = new Node(token);
            nextToken();

            //insert left expression(or term+expression)
            node.getChildren().add(left);

            Node comparison = parseComparison();
            if(comparison != null){
                //insert right comparison
                node.getChildren().add(comparison);
            }else{
                throw new ParserException("Expected a comparison after ["+node.getToken().value+"] but found ["+token+"] !");
            }

            Node termAndexpression = parseLogicAndComparison(node);
            if(termAndexpression != null){
                return termAndexpression;
            }else{
                return node;
            }
        }else{
            return null;
        }
    }

    private Node parseComparison() throws IOException, LexerException, ParserException{
        Node expression = parseExpression();

        if(token != null && token.type == Type.OPERATOR_L){
            Node node = new Node(token);
            nextToken();

            node.getChildren().add(expression);

            Node right = parseExpression();
            if(right == null)
                throw new ParserException("Tried to parse expression after '"+token+"' but failed!");
            else{
                node.getChildren().add(right);
                return node;
            }
        }else{
            return expression;
        }
    }

    private Node parseExpression() throws IOException, LexerException, ParserException{
        Node term = parseTerm();

        Node parent = parseTermAndExpression(term);
        if(parent != null){
            return parent;
        } else {
            return term;
        }
    }

    private Node parseTermAndExpression(Node left) throws IOException, LexerException, ParserException{
        if(token != null && token.type == Type.OPERATOR_A
                &&("+".equals(token.value) || "-".equals(token.value))){
            Node node = new Node(token);
            nextToken();

            //insert left expression(or term+expression)
            node.getChildren().add(left);

            Node term = parseTerm();
            if(term != null){
                //insert right term
                node.getChildren().add(term);
            }else{
                throw new ParserException("Expected a term after ["+node.getToken().value+"] but found ["+token+"] !");
            }

            Node termAndexpression = parseTermAndExpression(node);
            if(termAndexpression != null){
                return termAndexpression;
            }else{
                return node;
            }
        }else{
            return null;
        }
    }

    private Node parseTerm() throws IOException, LexerException, ParserException{
        Node factor = parseFactor();

        Node parent = parseFactorAndTerm(factor);
        if(parent != null){
            return parent;
        }else{
            return factor;
        }
    }

    private Node parseFactorAndTerm(Node left) throws IOException, LexerException, ParserException{
        if(token != null && token.type == Type.OPERATOR_A
                && ("*".equals(token.value) || "/".equals(token.value))){
            Node node = new Node(token);
            nextToken();

            node.getChildren().add(left);

            Node factor = parseFactor();
            if(factor != null){
                node.getChildren().add(factor);
            }else{
                throw new ParserException("Expected a factor after ["+node.getToken().value+"] but found ["+token+"] !");
            }

            Node factorAndTerm = parseFactorAndTerm(node);
            if(factorAndTerm != null){
                return factorAndTerm;
            }else{
                return node;
            }
        } else {
            return null;
        }
    }

    private Node parseFactor() throws IOException, LexerException, ParserException {
        Node idNode = parseId();
        if(idNode != null)
            return idNode;

        if(token == null)
            return null;

        if(token.type == Type.OPERATOR && "(".equals(token.value)){
            nextToken();

            Node expression = parseLogic();

            if(token == null || token.type != Type.OPERATOR || !")".equals(token.value)){
                throw new ParserException("Expected ')' but found "+token);
            }
            nextToken();

            return expression;
        }

        //do not process command as an Id
        if(token.type == Type.ID
                && ((String) token.value).charAt(0) == '#'){
            return null;
        }

        if (token.type == Type.GID
                || token.type == Type.OPERATOR_A
                || token.type == Type.OPERATOR_L
                || token.type.isLiteral()) {

            Node node = new Node(token);
            nextToken();
            return node;
        }

        throw new ParserException("Unknown token "+ token);
    }

    private Node parseId() throws IOException, LexerException, ParserException {
        if(token.type == Type.ID){
            Deque<Node> deque = new LinkedList<>();

            Token idToken = token;
            nextToken();

            if("(".equals(token.value)){//fuction call
                nextToken();

                Node call = new Node(new Token(Type.CALL, idToken.value));
                do{
                    call.getChildren().add(parseLogic());
                }while(",".equals(token.value));

                if(token == null || !")".equals(token))
                    throw new ParserException("Extected ')' but end of stream is reached.");

                nextToken();

                deque.addLast(call);
            }else if(".".equals(token.value)){//id
                deque.addLast(new Node(idToken));
            }else{
                return new Node(idToken);
            }

            while(".".equals(token.value)){
                nextToken();
                if(token.type != Type.ID)
                    throw new ParserException(token+" is not an id!");

                idToken = token;
                nextToken();

                if("(".equals(token.value)){//fuction call
                    nextToken();

                    Node call = new Node(new Token(Type.CALL, idToken.value));
                    call.getChildren().add(parseLogic());
                    while(",".equals(token.value)){
                        nextToken();
                        call.getChildren().add(parseLogic());
                    }

                    if(token == null || !")".equals(token.value))
                        throw new ParserException("Extected ')' but end of stream is reached.");
                    nextToken();

                    deque.addLast(call);
                }else{//id
                    deque.addLast(new Node(idToken));
                }
            }

            return parseId(deque);
        }else{
            return null;
        }
    }

    private Node parseId(Deque<Node> deque){
        Stack<Node> stack = new Stack<>();
        stack.push(deque.pop());

        while(!deque.isEmpty()){
            Node node = new Node(new Token(Type.OPERATOR, "."));
            node.getChildren().add(stack.pop());
            node.getChildren().add(deque.pop());

            stack.push(node);
        }

        return stack.pop();
    }

    public static void main(String[] ar) throws IOException, LexerException, ParserException{
        Charset charset = Charset.forName("UTF-8");
        String text = ""
                + "X = 5\n"
                + "str = \"abc\"\n"
                + "WHILE 1 > 0\n"
                + "    str = str + X\n"
                + "    IF player.in.health > 2 && player.in.health > 0\n"
                + "        #MESSAGE 3*4\n"
                + "    ELSE\n"
                + "        #MESSAGE str\n"
                + "    ENDIF\n"
                + "    #MESSAGE player.in.hasPermission(x, 2+3, 5 > 4)\n"
                + "    X = X - 1\n"
                + "    IF X < 0\n"
                + "        #STOP\n"
                + "    ENDIF\n"
                + "    #WAIT 1\n"
                + "ENDWHILE";

       // String text = "#MESSAGE \"빰빰\"";
        System.out.println("original: \n"+text);

        Lexer lexer = new Lexer(text, charset);
        Parser parser = new Parser(lexer);

        Node root = parser.parse();
        System.out.println(root.toString());

        JFrame frame = new JFrame("Manual Nodes");
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("Root");
        setNode(rootNode, root);
        JTree tree = new JTree(rootNode);
        JScrollPane scrollPane = new JScrollPane(tree);
        frame.getContentPane().add(scrollPane, BorderLayout.CENTER);
        frame.setSize(300, 150);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    private static void setNode(DefaultMutableTreeNode parent, Node node){
        if(node.getChildren().isEmpty()){
            parent.add(new DefaultMutableTreeNode(node.getToken().value));
        }else{
            DefaultMutableTreeNode holder = new DefaultMutableTreeNode(node.getToken().value);
            for(Node child : node.getChildren()){
                setNode(holder, child);
                parent.add(holder);
            }
        }
    }
}
