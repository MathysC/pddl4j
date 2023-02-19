(define (domain GRAPHCOLOR)

	(:requirements :strips :typing)

	(:types
		color vertice
	)
	(:predicates
		(connected ?a - vertice ?b - vertice) ; connect two vertices from 'a' to 'b'
		(hasColor ?v - vertice ?c - color) ; vertice 'v' has color 'c'
		(colored ?v - vertice) ; vertice 'v' is colored
		(notColored ?v - vertice) ; vertice 'v' is notcolored : This is used as precondition because we can’t have negative precondition ( not (colored ?v))
		(different ?a - color ?b - color) ; color 'a' is different from 'b'
		(differentVertices ?x - vertice ?y - vertice) ; for verification, true only if two connected vertices have non similar color
	)

	(:action connect ; Connect two vertices from 'a' to 'b' and from 'b' to 'a'
		:parameters (?a - vertice ?b - vertice)
		:precondition(and
			(connected ?a ?b)
		)
		:effect ( and
			(connected ?b ?a)
		)
	)

	(:action reverse ;  color 'a' is different from 'b' and 'b' from 'a'
		:parameters (?a - color ?b - color)
		:precondition (and
			(different ?a ?b)
		)
		:effect (and
			(different ?b ?a)
		)
	)

	(:action paintVertices ; paint two connected vertices when both are not colored.
		:parameters (?x - vertice ?y - vertice ?a - color ?b - color)
		:precondition (and
			(connected ?x ?y)
			(different ?a ?b)
			(notColored ?x)
			(notColored ?y)
		)
		:effect (and
			(not (notColored ?x)) ; They are ACTUALLY colored
			(colored ?x)
			(hasColor ?x ?a)
			(not (notColored ?y))
			(colored ?y)
			(hasColor ?y ?b)

		)
	)

	(:action paintVertice ; paint one vertice connected to a colored one.
		:parameters (?x - vertice ?y - vertice ?a - color ?b - color)
		:precondition (and
			(connected ?x ?y)
			(different ?a ?b)
			(notColored ?x)
			(hasColor ?y ?b)
		)
		:effect (and
			(not (notColored ?x))
			(colored ?x)
			(hasColor ?x ?a)
		)
	)

	(:action verification ; check if two connected vertices have different color.
		:parameters (?x - vertice ?y - vertice ?a - color ?b - color)
		:precondition (and
			(different ?a ?b)
			(hasColor ?x ?a)
			(hasColor ?y ?b)
			(connected ?x ?y)
		)
		:effect (and
			(differentVertices ?x ?y)
		)
	)

)