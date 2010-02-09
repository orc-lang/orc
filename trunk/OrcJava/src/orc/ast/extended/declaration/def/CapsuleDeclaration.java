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
import orc.ast.extended.visitor.Visitor;
import orc.ast.simple.expression.Expression;
import orc.error.compiletime.CompilationException;

/**
 * This class is responsible for transforming a capsule declaration
 * to the original Orc syntax. The details of this transformation can
 * be found at: 
 * <a href="http://orc.csres.utexas.edu/wiki/Wiki.jsp?page=OrcCapsules">OrcCapsules</a>
 * Feb 4, 2010
 * @author amshali
 */
public class CapsuleDeclaration extends Declaration {
	/**
	 * Capsule name
	 */
	public String name;
	/**
	 * list of formal parameters
	 */
	public List<List<Pattern>> formals;
	/**
	 * body of the capsule
	 */
	public orc.ast.extended.expression.Expression body;
	/**
	 * type of formal parameters
	 */
	public List<String> typeFormals = null;

	public CapsuleDeclaration(String name, List<List<Pattern>> formals, orc.ast.extended.expression.Expression body, List<String> typeFormals) {
		this.name = name;
		this.formals = formals;
		this.body = body;
		this.typeFormals = typeFormals;
	}

	@Override
	public Expression bindto(Expression target) throws CompilationException {

		body = makeNewBody(body, new ArrayList<String>());

		DefMemberClause defMemberClause = new DefMemberClause(name, formals, body, null, typeFormals);

		/* create a list of definitions with only one definition in it.
		* which is the capsule definition.
		*/
		List<DefMember> defs = new ArrayList<DefMember>();
		defs.add(defMemberClause);
		DefsDeclaration dd = new DefsDeclaration(defs);
		/*
		 * use a ValDeclaration to shadow the original definition with 
		 * a protected Site closure. 
		 */
		ValDeclaration vald = new ValDeclaration(new VariablePattern(name), new Call(new Name("Site"), new Name(name)));
		return dd.bindto(vald.bindto(target));
	}

	@Override
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}

	/**
	 * This function recursively looks for definitions inside the 
	 * capsule declaration (body) and put them all in defFunctions list.
	 * At the end  
	 * @param body Body of the capsule declaration.
	 * @param defFunctions List of definitions inside the capsule.
	 * @return Returns a new body
	 * @throws CompilationException
	 */
	private orc.ast.extended.expression.Expression makeNewBody(orc.ast.extended.expression.Expression body, List<String> defFunctions) throws CompilationException {
		if (body instanceof Declare) {
			Declare decl = (Declare) body;
			Declaration defs = decl.d;
			if (defs instanceof DefsDeclaration) {
				for (DefMember d : ((DefsDeclaration) defs).defs) {
					if (d instanceof DefMemberClause) {
						defFunctions.add(d.name);
					}
				}
			}
			return new Declare(decl.d, makeNewBody(decl.e, defFunctions));
		} else {
			if (defFunctions.size() == 0) {
				CompilationException exc = new CompilationException("A capsule must contain at least one def");
				exc.setSourceLocation(this.getSourceLocation());
				throw exc;
			} else {
				List<orc.ast.extended.expression.Expression> recordArgs = makeRecordArgs(defFunctions);
				Call recordCall = new Call(new Name("Record"), recordArgs);

				// Semantics: body as active process
				return new Parallel(new Sequential(body, new Stop()), recordCall);

				// Alternative semantics: body as initializer
				// return new Otherwise(new Sequential(body, new Stop()), recordCall);
			}
		}
	}

	/**
	 * This method gets a list of definitions, defFunctions, and returns 
	 * a Record site with the definitions protected by Site.
	 * @param defFunctions
	 * @return returns the a Record site with the name of definitions and the 
	 * definitions protected by a Site.
	 */
	private List<orc.ast.extended.expression.Expression> makeRecordArgs(List<String> defFunctions) {
		List<orc.ast.extended.expression.Expression> args = new ArrayList<orc.ast.extended.expression.Expression>();
		for (String s : defFunctions) {
			args.add(new Literal(s));
			args.add(new Call(new Name("Site"), new Name(s)));
		}
		return args;
	}
}
