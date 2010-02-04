//
// DefCapsuleClause.java -- Java class DefCapsuleClause
// Project OrcJava
//
// $Id$
//
// Created by amshali on Feb 4, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.ast.extended.declaration.def;

import java.util.ArrayList;
import java.util.List;

import orc.ast.extended.Visitor;
import orc.ast.extended.declaration.Declaration;
import orc.ast.extended.declaration.DefsDeclaration;
import orc.ast.extended.declaration.ValDeclaration;
import orc.ast.extended.expression.Call;
import orc.ast.extended.expression.Declare;
import orc.ast.extended.expression.Literal;
import orc.ast.extended.expression.Name;
import orc.ast.extended.expression.Parallel;
import orc.ast.extended.expression.Sequential;
import orc.ast.extended.expression.Stop;
import orc.ast.extended.pattern.Pattern;
import orc.ast.extended.pattern.VariablePattern;
import orc.ast.simple.expression.Expression;
import orc.error.compiletime.CompilationException;

/**
 * 
 * Feb 4, 2010
 * @author amshali
 */
public class CapsuleDeclaration extends Declaration {
	public String name;
	public List<List<Pattern>> formals;
	public orc.ast.extended.expression.Expression body;
	public List<String> typeFormals = null;

	public CapsuleDeclaration(String name, List<List<Pattern>> formals, orc.ast.extended.expression.Expression body, List<String> typeFormals) {
		this.name = name;
		this.formals = formals;
		this.body = body;
		this.typeFormals = typeFormals;
	}

	@Override
	public Expression bindto(Expression target) throws CompilationException {
		if (body instanceof Declare) 
			makeNewBody((Declare) body, new ArrayList<String>());
		else 
			System.err.println("Capsule does not have a Declare body"); //FIXME
		DefMemberClause defMemberClause = new DefMemberClause(name, formals, body, 
				null, typeFormals);
		
		List<DefMember> defs = new ArrayList<DefMember>();
		defs.add(defMemberClause);
		DefsDeclaration dd = new DefsDeclaration(defs);
		ValDeclaration vald = new ValDeclaration(new VariablePattern(name), 
				new Call(new Name("Site"), 
				new Name(name)));
		return dd.bindto(vald.bindto(target));
	}

	@Override
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}

	private void makeNewBody(Declare declare, List<String> defFunctions) {
		Declaration defs = declare.d;
		orc.ast.extended.expression.Expression e = declare.e;
		if (defs instanceof DefsDeclaration) {
			for (DefMember d : ((DefsDeclaration) defs).defs) {
				if (d instanceof DefMemberClause) {
					defFunctions.add(d.name);
				}
			}
		}
		if (e instanceof Declare) {
			makeNewBody((Declare) e, defFunctions);
		} else {
			List<orc.ast.extended.expression.Expression> recordArgs = makeRecordArgs(defFunctions);
			Call recordCall = new Call(new Name("Record"), recordArgs);
			Parallel parallel = new Parallel(new Sequential(e, new Stop()), recordCall);
			declare.e = parallel;
		}
	}

	private List<orc.ast.extended.expression.Expression> makeRecordArgs(List<String> defFunctions) {
		List<orc.ast.extended.expression.Expression> args = new ArrayList<orc.ast.extended.expression.Expression>();
		for (String s : defFunctions) {
			args.add(new Literal(s));
			args.add(new Call(new Name("Site"), new Name(s)));
		}
		return args;
	}
}
