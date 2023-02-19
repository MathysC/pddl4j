(define (domain HANOI)
	(:requirements :strips :typing)
	(:types
		disk rod
	)

	(:predicates
		(ondisk ?a - disk ?b - disk) ; a disk 'a' is on disk 'b'
		(clearDisk ?d - disk) ; a disk 'd' has nothing on top of it
		(clearRod ?r - rod) ; a rod 'r' has nothing on it
		(onrod ?d - disk ?r - rod) ; a disk can't be on top and the last one at the same time
		(handempty) ; the hand is empty
		(holding ?d - disk) ; the hand is holding the disk 'd'
		(bigger ?a -disk ?b - disk) ; the disk a is bigger than b
	)

	(:action unstack ;unstack a disk 'd' from a rod 'r' with at least two disk on it 'a' and 'b' (with 'a' on 'b')
		:parameters (?a - disk ?b - disk)
		:precondition (and (handempty) (clearDisk ?a) (ondisk ?a ?b))
		:effect (and (holding ?a) 
			(not (clearDisk ?a)) 
			(not (handempty))
			(not (ondisk ?a ?b))
			(clearDisk ?b))
	)

	(:action stack ;Put down a disk 'a' on a rod 'r' with at least already one disk 'b'on top of it
		:parameters (?a - disk ?b - disk)
		:precondition (and (holding ?a) (bigger ?b ?a) (clearDisk ?b))
		:effect (and (handempty)
			(not (clearDisk ?b))
			(clearDisk ?a)
			(ondisk ?a ?b)
			(not (holding ?a))
		)
	)

	(:action pick-up ;take a disk 'a' that is the last disk of rod 'r'
		:parameters ( ?d - disk ?r - rod)
		:precondition (and(handempty) (clearDisk ?d) (onRod ?d ?r))
		:effect (and (holding ?d)
			(clearRod ?r)
			(not (clearDisk ?d))
			(not (onRod ?d ?r))
			(not (handempty))
		)
	)

	(:action put-down ; put the disk 'd' on the rod 'r' as the last and only disk of rod 'r'
		:parameters ( ?d - disk ?r - rod)
		:precondition (and (clearRod ?r) (holding ?d))
		:effect (and (not (clearRod ?r))
			(clearDisk ?d)
			(onrod ?d ?r)
			(handempty)
			(not (holding ?d))
		)
	)

)