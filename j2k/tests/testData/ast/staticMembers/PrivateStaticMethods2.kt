class A() {
    public fun foo() {
        privateStatic1()
        privateStatic2()
    }

    class object {

        public fun publicStatic() {
            privateStatic1()
        }

        private fun privateStatic1() {
        }
        private fun privateStatic2() {
        }
    }
}