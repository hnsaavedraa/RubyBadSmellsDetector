import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import java.util.*;

class FunctionCreated {
    public String name;
    public int line;
    public int column;

    public FunctionCreated(String name, int line, int column){
        this.name = name;
        this.line = line;
        this.column = column;
    }
}
public class VisitorRuby<T> extends RubyBaseVisitor<T> {
    public static int returnsCounter;
    public static int chainsCounter;
    public static int conditionalsCounter;
    public static int returnsFunctionLine;
    public static int returnsFunctionColumn;
    public static String returnsFunctionName;
    public static String conditionalVariable;
    public static ArrayList<FunctionCreated> functionsCreated = new ArrayList<FunctionCreated>();
    public CodeSmellsManager manager;
    VisitorRuby(ArrayList<Integer> _enableSmells) {
        manager = new CodeSmellsManager(_enableSmells);
    }
    VisitorRuby() {
        manager = new CodeSmellsManager();
    }

    @Override public T visitFunction_definition_header(RubyParser.Function_definition_headerContext ctx) {
        returnsCounter = 0;
        returnsFunctionLine = ctx.start.getLine();
        returnsFunctionColumn = ctx.start.getCharPositionInLine();
        returnsFunctionName = ctx.getChild(1).getChild(0).getChild(0).toString();
        FunctionCreated func = new FunctionCreated(returnsFunctionName,returnsFunctionColumn,returnsFunctionLine);
        functionsCreated.add(func);
        
        return visitChildren(ctx);
    }

    @Override
    public T visitReturn_statement(RubyParser.Return_statementContext ctx) {
        if(returnsCounter == 0 ){
            returnsCounter += 1;
        }else{
            returnsCounter += 1;
            RuleContext StmtCtx = ctx.parent.parent.parent.parent;
            if(StmtCtx.equals("class RubyParser$Function_if_statementContext") ||
                StmtCtx.getClass().toString().equals("class RubyParser$Function_definitionContext") ||
                StmtCtx.getClass().toString().equals("class RubyParser$Function_if_elsif_statementContext") ||
                StmtCtx.getClass().toString().equals("class RubyParser$Function_definitionContext") ||
                StmtCtx.getClass().toString().equals("class RubyParser$Function_unless_statementContext") ||
                StmtCtx.parent.getClass().toString().equals("class RubyParser$Function_unless_statementContext") ||
                StmtCtx.parent.getClass().toString().equals("class RubyParser$Function_if_elsif_statementContext")){
                int line = ctx.start.getLine();
                int column = ctx.start.getCharPositionInLine();
                String message = "\nMal olor en la funcion: \'" + returnsFunctionName + "\' en Linea: "+ returnsFunctionLine + ", Columna: " + returnsFunctionColumn + 
                                    "\nSe encontraron multiples returns, primer return multiple Linea: "+ line +" Columna: " + column + "\n"
                                    + "Se recomienda crear una variable unica a retornar y asignarle a esto los valores que se pensaban retornar, posteriormente retornar esta variable al final de la funcion\n";

                manager.AddCodeSmell(SMELL.MultipleReturn, line, column, message);

            }
        }
        String StmtCtx = ctx.parent.parent.parent.parent.getClass().toString();
        if(StmtCtx.equals("class RubyParser$Function_while_statementContext")){
            int line = ctx.start.getLine();
            int column = ctx.start.getCharPositionInLine();
            String message ="\nMal olor encontrado, return dentro de una estructura de bucle en Linea : " + line + " Columna " + column + "\n"
                        + "Se recomienda no tener returns en estructuras de bucle, declare una variable donde guarde el resultado que desea y retornelo fuera de la estructura.\n";
            manager.AddCodeSmell(SMELL.ReturnInLoop, line, column, message);
        }

        if(StmtCtx.equals("class RubyParser$Function_for_statementContext")){
            int line = ctx.start.getLine();
            int column = ctx.start.getCharPositionInLine();
            String message ="\nMal olor encontrado, return dentro de una estructura de bucle en Linea : " + line + " Columna " + column + "\n"
                        + "Se recomienda no tener returns en estructuras de bucle, declare una variable donde guarde el resultado que desea y retornelo fuera de la estructura.\n";
            manager.AddCodeSmell(SMELL.ReturnInLoop, line, column, message);
        }
        // else if(StmtCtx.equals("class RubyParser$If_elsif_statementContext")){
        //     System.out.println("elsif");
        // }        
        String inFunction = ctx.parent.parent.parent.getClass().toString();
        //System.out.println(inFunction);
        if(!(inFunction.equals("class RubyParser$Function_statement_bodyContext") ||
                inFunction.equals("class RubyParser$Function_definition_bodyContext"))){
            int line = ctx.start.getLine()+1;
            int column = ctx.start.getCharPositionInLine();
            String message ="\nMal olor encontrado, codigo inalcanzable en Linea: " + line + " Columna " + column + "\n"
                        + "Se recomienda mover o eliminar el codigo que se encuentra despues del return.\n";
            manager.AddCodeSmell(SMELL.DeadCodeReturn, line, column, message);
        }

        return super.visitChildren(ctx);
    }

    @Override
    public T visitExpression(RubyParser.ExpressionContext ctx) {
        if((ctx.getChild(0).getClass().toString()).equals("class RubyParser$Function_chainContext")){
            chainsCounter = 0;
        }
        return super.visitChildren(ctx);
    }

    @Override
    public T visitFunction_chain(RubyParser.Function_chainContext ctx) {
        if(chainsCounter < 5){
            chainsCounter += 1;
        }else if(chainsCounter == 5){
            chainsCounter += 1;
            int line = ctx.start.getLine();
            int column = ctx.start.getCharPositionInLine();
            String message = "\nMal olor encontrado, muchas funciones encadenadas en Linea: " + line + " Columna: " + column + "\n"
                                + "Se recomienda dividir los encadenamientos en diferentes variables, donde como mucho se encadenen cuatro funciones, para que el codigo sea mas comprensible y facil de mantener\n";
            manager.AddCodeSmell(SMELL.ExtremeChains, line, column, message);
        }
        return super.visitChildren(ctx);
    }
    
    @Override 
    public T visitFunction_inline_call(RubyParser.Function_inline_callContext ctx) { 
       
        String functionName = ctx.getChild(0).getChild(0).getChild(0).getChild(0).toString();

        Iterator<FunctionCreated> itr = functionsCreated.iterator();
        while (itr.hasNext()) {
            FunctionCreated function = itr.next();
            if (function.name.equals(functionName)) {
                itr.remove();
            }
        }

        return visitChildren(ctx); 
    }

    @Override 
    public T visitTerminator(RubyParser.TerminatorContext ctx) { 
       if(ctx.getChild(ctx.getChildCount() - 1).toString().equals("<EOF>")){
             for (FunctionCreated x : functionsCreated){
                int line = x.line;
                int column = x.column;
                String message = "\nMal olor encontrado, la funcion \'" + x.name + "\' en linea " + line + " Columna: " + column + " nunca es llamada\n"
                                + "Se recomienda crear un llamado a la funcion o en caso de que no sea necesaria eliminarla.\n";
                 manager.AddCodeSmell(SMELL.FunctionsNotCalled, line, column, message);              
            } 
        } 
        return super.visitChildren(ctx); 
    }

    @Override
    public T visitIf_statement(RubyParser.If_statementContext ctx) {
        conditionalsCounter = 0;
        ParseTree comparison = ctx.getChild(1).getChild(0).getChild(1).getChild(0).getChild(0);
        conditionalVariable = comparison.getChild(0).getChild(0).getChild(0).getChild(0).getChild(0).toString();
        return visitChildren(ctx);
    }

    @Override
    public T visitIf_elsif_statement(RubyParser.If_elsif_statementContext ctx) {
        ParseTree comparison = ctx.getChild(1).getChild(0).getChild(1).getChild(0).getChild(0);
        String auxComparison = comparison.getChild(0).getChild(0).getChild(0).getChild(0).getChild(0).toString();
        if(auxComparison.equals(conditionalVariable)){
            if(conditionalsCounter < 4){
                conditionalsCounter += 1;
            }else if(conditionalsCounter == 4){
                conditionalsCounter += 1;
                int line = ctx.start.getLine();
                int column = ctx.start.getCharPositionInLine();
                String message = "\nMal olor encontrado, condicionales muy largos en Linea: " + line + " Columna: " + column + " para la variable \'" + auxComparison + "\'.\n"
                        + "Se recomienda la creacion de un objeto, donde pueda mapear las diferentes opciones de la variable, para asi ingresar a estas con mayor eficacia.\n";
                manager.AddCodeSmell(SMELL.LongConditionals, line, column, message);
            }
        }
        return visitChildren(ctx);
    }
}

