(define (problem GraphColor-0-1) (:domain GRAPHCOLOR)
(:objects 
    x1 x2 x3 - vertice
    a b c - color
)

(:init
    (connected x1 x2)
    (connected x2 x3)
    (connected x3 x1)
    (notColored x1)
    (notColored x2)
    (notColored x3)
    (different a b)
    (different a c)
    (different b c)

)

(:goal (and
    (colored x1)
    (colored x2)
    (colored x3)
    (differentVertices x1 x2)
    (differentVertices x2 x3)
    (differentVertices x1 x3)
))
)
