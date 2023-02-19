(define (problem csp-0-1) (:domain CSP)
    (:objects
        x1 x2 x3 x4 x5 - vertice 
        a b c - value
    )

    (:init
        (constrained x1 x3 a b)
        (constrained x1 x3 b a)
        (constrained x1 x3 c a)
        (constrained x2 x3 a a)
        (constrained x2 x3 b a)
        (constrained x3 x4 a a)
        (constrained x4 x5 a b)
        (constrained x3 x5 a b)
        (possibleValue x1 a)
        (possibleValue x1 b)
        (possibleValue x1 c)
        (possibleValue x2 a)
        (possibleValue x2 b)
        (possibleValue x2 c)
        (possibleValue x3 a)
        (possibleValue x3 b)
        (possibleValue x4 a)
        (possibleValue x4 b)
        (possibleValue x5 a)
        (possibleValue x5 b)
        (valuable x1)
        (valuable x2)
        (valuable x3)
        (valuable x4)
        (valuable x5)


    )

    (:goal
        (and
            (notValuable x1)
            (notValuable x2)
            (notValuable x3)
            (notValuable x4)
            (notValuable x5)
        )
    )

)