fun foo() {
    { -> a}

    {(a -> a}
    {(a : ) -> a}
    {(a : A) : -> a}
    {(a) : T -> }

    {(a, ) -> a}
    {(a : A, , a : B) -> a}
    {(a : A, , , a) : T -> a}

    {T.t(a) -> a}
    {T.t -(a : A) -> a}

    {a : b, -> f}
    {a : , c -> f}
    {a :  -> f}
    {a, -> f}

    {a : b, }
    {a : , }

    {T.a : b -> f}

    {(a, b) }
    {T.(a, b) }
    {(a: Int, )}
    {a, }
}