(define (domain CSP)

	(:requirements :strips :typing)

	(:types
		vertice value
	)

	(:predicates
		(constrained ?x - vertice ?y - vertice ?a - value ?b -value) ; a pair (t; r) with  r belonging to R `t` is a tuple of vertices and `r` a tuple of possible value from each vertices.
		(possibleValue ?x - vertice ?a - value) ; a possible value 'a' of vertice 'x'.
		(valuable ?x - vertice) ; vertice 'x' does not contain a value yet.
		(notValuable ?x - vertice) ; vertice 'x' has a final value.
		(hasValue ?x - vertice ?a - value) ; vertice 'x' has value 'a'
	)

	(:action giveConstraint ; connect two constraint in both ways (must be first made in INIT with (constrained ?x ?y ?a ?b))
		:parameters (?x - vertice ?y - vertice ?a - value ?b - value)
		:precondition (and
			(constrained ?x ?y ?a ?b)
		)
		:effect (and
			(constrained ?y ?x ?b ?a)
		)
	)

	(:action giveValues ; give value 'a' to vertice 'x' and 'b' to 'y'
		:parameters (?x - vertice ?y - vertice ?a - value ?b - value)
		:precondition (and
			(possibleValue ?x ?a)
			(constrained ?x ?y ?a ?b)
			(valuable ?x)
			(valuable ?y)
		)
		:effect (and
			(not (valuable ?x))
			(notValuable ?x) ; Have a final value on a vertice
			(hasValue ?x ?a)
			(not (valuable ?y))
			(notValuable ?y) ; Have a final value on a vertice
			(hasValue ?y ?b)
		)
	)

	(:action giveValue ; give value '?a' to vertice '?x' if 'y' has value 'b'
		:parameters (?x - vertice ?y - vertice ?a - value ?b - value)
		:precondition (and
			(possibleValue ?x ?a)
			(constrained ?x ?y ?a ?b)
			(valuable ?x)
			(hasValue ?y ?b)
		)
		:effect (and
			(not (valuable ?x))
			(notValuable ?x) ; Have a final value on a vertice
			(hasValue ?x ?a)

		)
	)

)