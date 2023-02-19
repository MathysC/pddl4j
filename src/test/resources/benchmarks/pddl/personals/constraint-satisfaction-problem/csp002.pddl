(define (problem csp-0-2) (:domain CSP)
    (:objects
        x1 x2 x3 - vertice 
        a b c - value
    )

    (:init
        (valuable x1)
        (valuable x2)
        (valuable x3)
        (possibleValue x1 a)
        (possibleValue x1 b)
        (possibleValue x1 c)
        (possibleValue x2 a)
        (possibleValue x2 b)
        (possibleValue x2 c)
        (possibleValue x3 a)
        (possibleValue x3 b)
        (possibleValue x3 c)
        (constrained x1 x2 a b)
        (constrained x1 x2 a c)
        (constrained x1 x2 b a)
        (constrained x1 x2 b c)
        (constrained x1 x2 c a)
        (constrained x1 x2 c b)
        (constrained x1 x3 a b)
        (constrained x1 x3 a c)
        (constrained x1 x3 b a)
        (constrained x1 x3 b c)
        (constrained x1 x3 c a)
        (constrained x1 x3 c b)
        (constrained x2 x3 a b)
        (constrained x2 x3 a c)
        (constrained x2 x3 b a)
        (constrained x2 x3 b c)
        (constrained x2 x3 c a)
        (constrained x2 x3 c b)
    )

    (:goal
        (and
            (notValuable x1)
            (notValuable x2)
            (notValuable x3)
        )
    )

)