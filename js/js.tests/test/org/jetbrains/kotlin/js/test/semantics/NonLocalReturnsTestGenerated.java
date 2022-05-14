/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.js.test.semantics;

import com.intellij.testFramework.TestDataPath;
import org.jetbrains.kotlin.test.JUnit3RunnerWithInners;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.TargetBackend;
import org.jetbrains.kotlin.test.TestMetadata;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.regex.Pattern;

/** This class is generated by {@link org.jetbrains.kotlin.generators.tests.TestsPackage}. DO NOT MODIFY MANUALLY */
@SuppressWarnings("all")
@TestMetadata("compiler/testData/codegen/boxInline/nonLocalReturns")
@TestDataPath("$PROJECT_ROOT")
@RunWith(JUnit3RunnerWithInners.class)
public class NonLocalReturnsTestGenerated extends AbstractNonLocalReturnsTest {
    public void testAllFilesPresentInNonLocalReturns() throws Exception {
        KotlinTestUtils.assertAllTestsPresentByMetadata(this.getClass(), new File("compiler/testData/codegen/boxInline/nonLocalReturns"), Pattern.compile("^(.+)\\.kt$"), TargetBackend.JS, true);
    }

    @TestMetadata("explicitLocalReturn.kt")
    public void testExplicitLocalReturn() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/codegen/boxInline/nonLocalReturns/explicitLocalReturn.kt");
        doTest(fileName);
    }

    @TestMetadata("justReturnInLambda.kt")
    public void testJustReturnInLambda() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/codegen/boxInline/nonLocalReturns/justReturnInLambda.kt");
        doTest(fileName);
    }

    @TestMetadata("kt5199.kt")
    public void testKt5199() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/codegen/boxInline/nonLocalReturns/kt5199.kt");
        doTest(fileName);
    }

    @TestMetadata("kt8948.kt")
    public void testKt8948() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/codegen/boxInline/nonLocalReturns/kt8948.kt");
        doTest(fileName);
    }

    @TestMetadata("kt8948v2.kt")
    public void testKt8948v2() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/codegen/boxInline/nonLocalReturns/kt8948v2.kt");
        doTest(fileName);
    }

    @TestMetadata("nestedNonLocals.kt")
    public void testNestedNonLocals() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/codegen/boxInline/nonLocalReturns/nestedNonLocals.kt");
        doTest(fileName);
    }

    @TestMetadata("noInlineLocalReturn.kt")
    public void testNoInlineLocalReturn() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/codegen/boxInline/nonLocalReturns/noInlineLocalReturn.kt");
        doTest(fileName);
    }

    @TestMetadata("nonLocalReturnFromOuterLambda.kt")
    public void testNonLocalReturnFromOuterLambda() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/codegen/boxInline/nonLocalReturns/nonLocalReturnFromOuterLambda.kt");
        doTest(fileName);
    }

    @TestMetadata("propertyAccessors.kt")
    public void testPropertyAccessors() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/codegen/boxInline/nonLocalReturns/propertyAccessors.kt");
        doTest(fileName);
    }

    @TestMetadata("returnFromFunctionExpr.kt")
    public void testReturnFromFunctionExpr() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/codegen/boxInline/nonLocalReturns/returnFromFunctionExpr.kt");
        doTest(fileName);
    }

    @TestMetadata("simple.kt")
    public void testSimple() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/codegen/boxInline/nonLocalReturns/simple.kt");
        doTest(fileName);
    }

    @TestMetadata("simpleFunctional.kt")
    public void testSimpleFunctional() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/codegen/boxInline/nonLocalReturns/simpleFunctional.kt");
        doTest(fileName);
    }

    @TestMetadata("simpleVoid.kt")
    public void testSimpleVoid() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/codegen/boxInline/nonLocalReturns/simpleVoid.kt");
        doTest(fileName);
    }

    @TestMetadata("compiler/testData/codegen/boxInline/nonLocalReturns/deparenthesize")
    @TestDataPath("$PROJECT_ROOT")
    @RunWith(JUnit3RunnerWithInners.class)
    public static class Deparenthesize extends AbstractNonLocalReturnsTest {
        public void testAllFilesPresentInDeparenthesize() throws Exception {
            KotlinTestUtils.assertAllTestsPresentByMetadata(this.getClass(), new File("compiler/testData/codegen/boxInline/nonLocalReturns/deparenthesize"), Pattern.compile("^(.+)\\.kt$"), TargetBackend.JS, true);
        }

        @TestMetadata("bracket.kt")
        public void testBracket() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/codegen/boxInline/nonLocalReturns/deparenthesize/bracket.kt");
            doTest(fileName);
        }

        @TestMetadata("labeled.kt")
        public void testLabeled() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/codegen/boxInline/nonLocalReturns/deparenthesize/labeled.kt");
            doTest(fileName);
        }
    }

    @TestMetadata("compiler/testData/codegen/boxInline/nonLocalReturns/tryFinally")
    @TestDataPath("$PROJECT_ROOT")
    @RunWith(JUnit3RunnerWithInners.class)
    public static class TryFinally extends AbstractNonLocalReturnsTest {
        public void testAllFilesPresentInTryFinally() throws Exception {
            KotlinTestUtils.assertAllTestsPresentByMetadata(this.getClass(), new File("compiler/testData/codegen/boxInline/nonLocalReturns/tryFinally"), Pattern.compile("^(.+)\\.kt$"), TargetBackend.JS, true);
        }

        @TestMetadata("kt6956.kt")
        public void testKt6956() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/codegen/boxInline/nonLocalReturns/tryFinally/kt6956.kt");
            doTest(fileName);
        }

        @TestMetadata("kt7273.kt")
        public void testKt7273() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/codegen/boxInline/nonLocalReturns/tryFinally/kt7273.kt");
            doTest(fileName);
        }

        @TestMetadata("nonLocalReturnFromOuterLambda.kt")
        public void testNonLocalReturnFromOuterLambda() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/codegen/boxInline/nonLocalReturns/tryFinally/nonLocalReturnFromOuterLambda.kt");
            doTest(fileName);
        }

        @TestMetadata("compiler/testData/codegen/boxInline/nonLocalReturns/tryFinally/callSite")
        @TestDataPath("$PROJECT_ROOT")
        @RunWith(JUnit3RunnerWithInners.class)
        public static class CallSite extends AbstractNonLocalReturnsTest {
            public void testAllFilesPresentInCallSite() throws Exception {
                KotlinTestUtils.assertAllTestsPresentByMetadata(this.getClass(), new File("compiler/testData/codegen/boxInline/nonLocalReturns/tryFinally/callSite"), Pattern.compile("^(.+)\\.kt$"), TargetBackend.JS, true);
            }

            @TestMetadata("callSite.kt")
            public void testCallSite() throws Exception {
                String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/codegen/boxInline/nonLocalReturns/tryFinally/callSite/callSite.kt");
                doTest(fileName);
            }

            @TestMetadata("callSiteComplex.kt")
            public void testCallSiteComplex() throws Exception {
                String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/codegen/boxInline/nonLocalReturns/tryFinally/callSite/callSiteComplex.kt");
                doTest(fileName);
            }

            @TestMetadata("exceptionTableSplit.kt")
            public void testExceptionTableSplit() throws Exception {
                String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/codegen/boxInline/nonLocalReturns/tryFinally/callSite/exceptionTableSplit.kt");
                doTest(fileName);
            }

            @TestMetadata("exceptionTableSplitNoReturn.kt")
            public void testExceptionTableSplitNoReturn() throws Exception {
                String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/codegen/boxInline/nonLocalReturns/tryFinally/callSite/exceptionTableSplitNoReturn.kt");
                doTest(fileName);
            }

            @TestMetadata("finallyInFinally.kt")
            public void testFinallyInFinally() throws Exception {
                String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/codegen/boxInline/nonLocalReturns/tryFinally/callSite/finallyInFinally.kt");
                doTest(fileName);
            }

            @TestMetadata("wrongVarInterval.kt")
            public void testWrongVarInterval() throws Exception {
                String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/codegen/boxInline/nonLocalReturns/tryFinally/callSite/wrongVarInterval.kt");
                doTest(fileName);
            }
        }

        @TestMetadata("compiler/testData/codegen/boxInline/nonLocalReturns/tryFinally/chained")
        @TestDataPath("$PROJECT_ROOT")
        @RunWith(JUnit3RunnerWithInners.class)
        public static class Chained extends AbstractNonLocalReturnsTest {
            public void testAllFilesPresentInChained() throws Exception {
                KotlinTestUtils.assertAllTestsPresentByMetadata(this.getClass(), new File("compiler/testData/codegen/boxInline/nonLocalReturns/tryFinally/chained"), Pattern.compile("^(.+)\\.kt$"), TargetBackend.JS, true);
            }

            @TestMetadata("finallyInFinally.kt")
            public void testFinallyInFinally() throws Exception {
                String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/codegen/boxInline/nonLocalReturns/tryFinally/chained/finallyInFinally.kt");
                doTest(fileName);
            }

            @TestMetadata("finallyInFinally2.kt")
            public void testFinallyInFinally2() throws Exception {
                String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/codegen/boxInline/nonLocalReturns/tryFinally/chained/finallyInFinally2.kt");
                doTest(fileName);
            }

            @TestMetadata("intReturn.kt")
            public void testIntReturn() throws Exception {
                String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/codegen/boxInline/nonLocalReturns/tryFinally/chained/intReturn.kt");
                doTest(fileName);
            }

            @TestMetadata("intReturnComplex.kt")
            public void testIntReturnComplex() throws Exception {
                String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/codegen/boxInline/nonLocalReturns/tryFinally/chained/intReturnComplex.kt");
                doTest(fileName);
            }

            @TestMetadata("intReturnComplex2.kt")
            public void testIntReturnComplex2() throws Exception {
                String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/codegen/boxInline/nonLocalReturns/tryFinally/chained/intReturnComplex2.kt");
                doTest(fileName);
            }

            @TestMetadata("intReturnComplex3.kt")
            public void testIntReturnComplex3() throws Exception {
                String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/codegen/boxInline/nonLocalReturns/tryFinally/chained/intReturnComplex3.kt");
                doTest(fileName);
            }

            @TestMetadata("intReturnComplex4.kt")
            public void testIntReturnComplex4() throws Exception {
                String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/codegen/boxInline/nonLocalReturns/tryFinally/chained/intReturnComplex4.kt");
                doTest(fileName);
            }

            @TestMetadata("nestedLambda.kt")
            public void testNestedLambda() throws Exception {
                String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/codegen/boxInline/nonLocalReturns/tryFinally/chained/nestedLambda.kt");
                doTest(fileName);
            }
        }

        @TestMetadata("compiler/testData/codegen/boxInline/nonLocalReturns/tryFinally/declSite")
        @TestDataPath("$PROJECT_ROOT")
        @RunWith(JUnit3RunnerWithInners.class)
        public static class DeclSite extends AbstractNonLocalReturnsTest {
            public void testAllFilesPresentInDeclSite() throws Exception {
                KotlinTestUtils.assertAllTestsPresentByMetadata(this.getClass(), new File("compiler/testData/codegen/boxInline/nonLocalReturns/tryFinally/declSite"), Pattern.compile("^(.+)\\.kt$"), TargetBackend.JS, true);
            }

            @TestMetadata("complex.kt")
            public void testComplex() throws Exception {
                String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/codegen/boxInline/nonLocalReturns/tryFinally/declSite/complex.kt");
                doTest(fileName);
            }

            @TestMetadata("intReturn.kt")
            public void testIntReturn() throws Exception {
                String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/codegen/boxInline/nonLocalReturns/tryFinally/declSite/intReturn.kt");
                doTest(fileName);
            }

            @TestMetadata("intReturnComplex.kt")
            public void testIntReturnComplex() throws Exception {
                String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/codegen/boxInline/nonLocalReturns/tryFinally/declSite/intReturnComplex.kt");
                doTest(fileName);
            }

            @TestMetadata("longReturn.kt")
            public void testLongReturn() throws Exception {
                String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/codegen/boxInline/nonLocalReturns/tryFinally/declSite/longReturn.kt");
                doTest(fileName);
            }

            @TestMetadata("nested.kt")
            public void testNested() throws Exception {
                String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/codegen/boxInline/nonLocalReturns/tryFinally/declSite/nested.kt");
                doTest(fileName);
            }

            @TestMetadata("returnInFinally.kt")
            public void testReturnInFinally() throws Exception {
                String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/codegen/boxInline/nonLocalReturns/tryFinally/declSite/returnInFinally.kt");
                doTest(fileName);
            }

            @TestMetadata("returnInTry.kt")
            public void testReturnInTry() throws Exception {
                String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/codegen/boxInline/nonLocalReturns/tryFinally/declSite/returnInTry.kt");
                doTest(fileName);
            }

            @TestMetadata("returnInTryAndFinally.kt")
            public void testReturnInTryAndFinally() throws Exception {
                String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/codegen/boxInline/nonLocalReturns/tryFinally/declSite/returnInTryAndFinally.kt");
                doTest(fileName);
            }

            @TestMetadata("severalInTry.kt")
            public void testSeveralInTry() throws Exception {
                String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/codegen/boxInline/nonLocalReturns/tryFinally/declSite/severalInTry.kt");
                doTest(fileName);
            }

            @TestMetadata("severalInTryComplex.kt")
            public void testSeveralInTryComplex() throws Exception {
                String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/codegen/boxInline/nonLocalReturns/tryFinally/declSite/severalInTryComplex.kt");
                doTest(fileName);
            }

            @TestMetadata("voidInlineFun.kt")
            public void testVoidInlineFun() throws Exception {
                String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/codegen/boxInline/nonLocalReturns/tryFinally/declSite/voidInlineFun.kt");
                doTest(fileName);
            }

            @TestMetadata("voidNonLocal.kt")
            public void testVoidNonLocal() throws Exception {
                String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/codegen/boxInline/nonLocalReturns/tryFinally/declSite/voidNonLocal.kt");
                doTest(fileName);
            }
        }

        @TestMetadata("compiler/testData/codegen/boxInline/nonLocalReturns/tryFinally/exceptionTable")
        @TestDataPath("$PROJECT_ROOT")
        @RunWith(JUnit3RunnerWithInners.class)
        public static class ExceptionTable extends AbstractNonLocalReturnsTest {
            public void testAllFilesPresentInExceptionTable() throws Exception {
                KotlinTestUtils.assertAllTestsPresentByMetadata(this.getClass(), new File("compiler/testData/codegen/boxInline/nonLocalReturns/tryFinally/exceptionTable"), Pattern.compile("^(.+)\\.kt$"), TargetBackend.JS, true);
            }

            @TestMetadata("break.kt")
            public void testBreak() throws Exception {
                String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/codegen/boxInline/nonLocalReturns/tryFinally/exceptionTable/break.kt");
                doTest(fileName);
            }

            @TestMetadata("continue.kt")
            public void testContinue() throws Exception {
                String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/codegen/boxInline/nonLocalReturns/tryFinally/exceptionTable/continue.kt");
                doTest(fileName);
            }

            @TestMetadata("exceptionInFinally.kt")
            public void testExceptionInFinally() throws Exception {
                String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/codegen/boxInline/nonLocalReturns/tryFinally/exceptionTable/exceptionInFinally.kt");
                doTest(fileName);
            }

            @TestMetadata("forInFinally.kt")
            public void testForInFinally() throws Exception {
                String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/codegen/boxInline/nonLocalReturns/tryFinally/exceptionTable/forInFinally.kt");
                doTest(fileName);
            }

            @TestMetadata("innerAndExternal.kt")
            public void testInnerAndExternal() throws Exception {
                String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/codegen/boxInline/nonLocalReturns/tryFinally/exceptionTable/innerAndExternal.kt");
                doTest(fileName);
            }

            @TestMetadata("innerAndExternalNested.kt")
            public void testInnerAndExternalNested() throws Exception {
                String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/codegen/boxInline/nonLocalReturns/tryFinally/exceptionTable/innerAndExternalNested.kt");
                doTest(fileName);
            }

            @TestMetadata("innerAndExternalSimple.kt")
            public void testInnerAndExternalSimple() throws Exception {
                String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/codegen/boxInline/nonLocalReturns/tryFinally/exceptionTable/innerAndExternalSimple.kt");
                doTest(fileName);
            }

            @TestMetadata("nested.kt")
            public void testNested() throws Exception {
                String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/codegen/boxInline/nonLocalReturns/tryFinally/exceptionTable/nested.kt");
                doTest(fileName);
            }

            @TestMetadata("nestedWithReturns.kt")
            public void testNestedWithReturns() throws Exception {
                String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/codegen/boxInline/nonLocalReturns/tryFinally/exceptionTable/nestedWithReturns.kt");
                doTest(fileName);
            }

            @TestMetadata("nestedWithReturnsSimple.kt")
            public void testNestedWithReturnsSimple() throws Exception {
                String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/codegen/boxInline/nonLocalReturns/tryFinally/exceptionTable/nestedWithReturnsSimple.kt");
                doTest(fileName);
            }

            @TestMetadata("noFinally.kt")
            public void testNoFinally() throws Exception {
                String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/codegen/boxInline/nonLocalReturns/tryFinally/exceptionTable/noFinally.kt");
                doTest(fileName);
            }

            @TestMetadata("severalCatchClause.kt")
            public void testSeveralCatchClause() throws Exception {
                String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/codegen/boxInline/nonLocalReturns/tryFinally/exceptionTable/severalCatchClause.kt");
                doTest(fileName);
            }

            @TestMetadata("simpleThrow.kt")
            public void testSimpleThrow() throws Exception {
                String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/codegen/boxInline/nonLocalReturns/tryFinally/exceptionTable/simpleThrow.kt");
                doTest(fileName);
            }

            @TestMetadata("synchonized.kt")
            public void testSynchonized() throws Exception {
                String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/codegen/boxInline/nonLocalReturns/tryFinally/exceptionTable/synchonized.kt");
                doTest(fileName);
            }

            @TestMetadata("throwInFinally.kt")
            public void testThrowInFinally() throws Exception {
                String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/codegen/boxInline/nonLocalReturns/tryFinally/exceptionTable/throwInFinally.kt");
                doTest(fileName);
            }

            @TestMetadata("tryCatchInFinally.kt")
            public void testTryCatchInFinally() throws Exception {
                String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/codegen/boxInline/nonLocalReturns/tryFinally/exceptionTable/tryCatchInFinally.kt");
                doTest(fileName);
            }
        }

        @TestMetadata("compiler/testData/codegen/boxInline/nonLocalReturns/tryFinally/variables")
        @TestDataPath("$PROJECT_ROOT")
        @RunWith(JUnit3RunnerWithInners.class)
        public static class Variables extends AbstractNonLocalReturnsTest {
            public void testAllFilesPresentInVariables() throws Exception {
                KotlinTestUtils.assertAllTestsPresentByMetadata(this.getClass(), new File("compiler/testData/codegen/boxInline/nonLocalReturns/tryFinally/variables"), Pattern.compile("^(.+)\\.kt$"), TargetBackend.JS, true);
            }

            @TestMetadata("kt7792.kt")
            public void testKt7792() throws Exception {
                String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/codegen/boxInline/nonLocalReturns/tryFinally/variables/kt7792.kt");
                doTest(fileName);
            }
        }
    }
}
