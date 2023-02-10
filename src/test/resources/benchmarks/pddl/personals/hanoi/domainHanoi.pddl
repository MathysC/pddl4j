(define (domain HANOI)
  (:requirements :strips :typing)
  (:types disk 
  	  rod)
  (:predicates	( on ?a - disk ?b - disk)
		(clear ?d - disk)
		(clear ?r - rod)
		(ontop ?d - disk ?r - rod)
		(handempty)
		(holding ?d - disk)
		
  )

  (:action PICK-UP
	:parameters 	(?d - disk ?r - rod)
	:precondition	(and (ontop ?d ?r) (handempty) (clear ?d))
	:effect		(and (holding ?d) (not (clear ?d)) (not (handempty)) (not ontop ?d ?r))
  )
)
