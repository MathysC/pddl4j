(define (problem HANOI-0-1) 
(:domain HANOI)
(:objects 
    r1 r2 r3 - rod
    d1 d2 d3 d4 - disk
)

(:init
    (bigger d4 d3) (bigger d4 d2) (bigger d4 d1)(bigger d3 d2) (bigger d3 d1) (bigger d2 d1)
    (onrod d4 r1)
    (ondisk d3 d4) (ondisk d2 d3) (ondisk d1 d2)
    (clearRod r2) (clearRod r3)
    (clearDisk d1)
    (handempty)
    
)

(:goal (and
    (onrod d4 r3) 
    (ondisk d3 d4) (ondisk d2 d3) (ondisk d1 d2)
))

)
