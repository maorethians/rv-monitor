package com.runtimeverification.rvmonitor.java.rvj.output.codedom.helper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.runtimeverification.rvmonitor.java.rvj.output.NotImplementedException;
import com.runtimeverification.rvmonitor.java.rvj.output.codedom.type.CodeType;

public class CodeFormatters {
	public static ICodeFormatter getDefault() {
		return new JavaCodeFormatter();
	}
}

class JavaCodeFormatter implements ICodeFormatter {
	private final Set<String> imports;
	private final List<String> lines;
	private StringBuilder line;
	private int indentlevel;
	private State state;
	
	public JavaCodeFormatter() {
		this.imports = new HashSet<String>();
		this.lines = new ArrayList<String>();
		this.line = new StringBuilder();
		this.indentlevel = 0;
		this.state = State.NEWLINE;
	}
	
	public boolean needsPrintVariableDescription() {
		return false;
	}
	
	public String getCode() {
		StringBuilder s = new StringBuilder();
		for (String l : this.lines) {
			s.append(l);
			s.append('\n');
		}
		if (this.line.length() > 0)
			s.append(this.line);
		s.append('\n');
		return s.toString();
	}
	
	public void addImport(CodeType type) {
		String fqdn = type.getPackageName() + "." + type.getClassName();
		this.imports.add(fqdn);
	}
	
	public void newline() {
		this.lines.add(this.line.toString());
		this.line = new StringBuilder();
		this.state = State.NEWLINE;
	}

	private void indent() {
		for (int i = 0; i < this.indentlevel; ++i)
			this.line.append('\t');
	}
	
	private void append(String str) {
		this.append(str, false, false);
	}
	
	private void append(String str, boolean nospace, boolean forcespace) {
		switch (this.state) {
		case NEWLINE:
			this.indent();
			break;
		case COMMENT:
			this.newline();
			this.indent();
			break;
		case KEYWORD:
		case OPERATOR:
		case LITERAL:
		case IDENTIFIER:
			if (!nospace)
				this.line.append(' ');
			break;
		case OPERATORSEMITIGHT:
		case OPERATORTIGHT:
			if (forcespace)
				this.line.append(' ');
			break;
		default:
			throw new NotImplementedException();
		}
		
		this.line.append(str);
	}
	
	public void push() {
		this.newline();
		this.indentlevel++;
	}

	public void pop() {
		this.indentlevel--;
	}

	public void openBlock() {
		boolean forcespace = this.state != State.NEWLINE;
	
		this.append("{", false, forcespace);
		this.push();
	}

	public void closeBlock() {
		this.pop();
		this.append("}");
		this.newline();
	}
	
	public void endOfStatement() {
		this.operator(";");
		this.newline();
	}
	
	public void comment(String cmt) {
		this.append("// " + cmt);
		this.state = State.COMMENT;
	}
	
	public void type(CodeType type) {
		this.addImport(type);
		this.identifier(type.getJavaType());
	}

	public void keyword(String kw) {
		this.append(kw);
		this.state = State.KEYWORD;
	}
	
	private boolean matchOperator(String[] haystack, String needle) {
		for (String h : haystack) {
			if (h.equals(needle))
				return true;
		}
		return false;
	}

	public void operator(String op) {
		String[] ltight = { ":", ",", ";", ".", ")", "++", "--" };
		// No space after an identifier or keyword; e.g., if (.
		String[] ltight_weak = { "(" };

		String[] rtight = { ".", "(", "!", "++", "--" };
		String[] rtight_weak = { ")" };

		boolean tightbefore = false;
		boolean tightafter = false;
		boolean semitightafter = false;
		
		tightbefore = this.matchOperator(ltight, op);
		tightafter = this.matchOperator(rtight, op);
		semitightafter = this.matchOperator(rtight_weak, op);
	
		if (!tightbefore) {
			switch (this.state) {
			case KEYWORD:
			case OPERATOR:
				break;
			default:
				tightbefore = this.matchOperator(ltight_weak, op);
				break;
			}
		}

		boolean forcespace = false;
		{
			switch (this.state) {
			case OPERATORSEMITIGHT:
				forcespace = true;
				break;
			default:
				break;
			}
		}
		
		this.append(op, tightbefore, forcespace);

		if (tightafter)
			this.state = State.OPERATORTIGHT;
		else if (semitightafter)
			this.state = State.OPERATORSEMITIGHT;
		else
			this.state = State.OPERATOR;
	}
	
	public void literal(String lt) {
		this.append(lt);
		this.state = State.LITERAL;
	}
	
	public void identifier(String id) {
		this.append(id);
		this.state = State.IDENTIFIER;
	}
	
	public void legacyExpr(String raw) {
		this.append(raw);
	}
	
	public void legacyStmt(String raw) {
		this.append(raw);
		this.newline();
	}
}