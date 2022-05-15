/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls;

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
@TestMetadata("compiler/testData/resolvedCalls")
@TestDataPath("$PROJECT_ROOT")
@RunWith(JUnit3RunnerWithInners.class)
public class ResolvedCallsTestGenerated extends AbstractResolvedCallsTest {
    public void testAllFilesPresentInResolvedCalls() throws Exception {
        KotlinTestUtils.assertAllTestsPresentByMetadata(this.getClass(), new File("compiler/testData/resolvedCalls"), Pattern.compile("^(.+)\\.kt$"), TargetBackend.ANY, true, "enhancedSignatures");
    }

    @TestMetadata("explicitReceiverIsDispatchReceiver.kt")
    public void testExplicitReceiverIsDispatchReceiver() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/resolvedCalls/explicitReceiverIsDispatchReceiver.kt");
        doTest(fileName);
    }

    @TestMetadata("explicitReceiverIsExtensionReceiver.kt")
    public void testExplicitReceiverIsExtensionReceiver() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/resolvedCalls/explicitReceiverIsExtensionReceiver.kt");
        doTest(fileName);
    }

    @TestMetadata("hasBothDispatchAndExtensionReceivers.kt")
    public void testHasBothDispatchAndExtensionReceivers() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/resolvedCalls/hasBothDispatchAndExtensionReceivers.kt");
        doTest(fileName);
    }

    @TestMetadata("hasBothDispatchAndExtensionReceiversWithoutExplicitReceiver.kt")
    public void testHasBothDispatchAndExtensionReceiversWithoutExplicitReceiver() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/resolvedCalls/hasBothDispatchAndExtensionReceiversWithoutExplicitReceiver.kt");
        doTest(fileName);
    }

    @TestMetadata("implicitReceiverIsDispatchReceiver.kt")
    public void testImplicitReceiverIsDispatchReceiver() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/resolvedCalls/implicitReceiverIsDispatchReceiver.kt");
        doTest(fileName);
    }

    @TestMetadata("implicitReceiverIsExtensionReceiver.kt")
    public void testImplicitReceiverIsExtensionReceiver() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/resolvedCalls/implicitReceiverIsExtensionReceiver.kt");
        doTest(fileName);
    }

    @TestMetadata("impliedThisNoExplicitReceiver.kt")
    public void testImpliedThisNoExplicitReceiver() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/resolvedCalls/impliedThisNoExplicitReceiver.kt");
        doTest(fileName);
    }

    @TestMetadata("simpleCall.kt")
    public void testSimpleCall() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/resolvedCalls/simpleCall.kt");
        doTest(fileName);
    }

    @TestMetadata("compiler/testData/resolvedCalls/arguments")
    @TestDataPath("$PROJECT_ROOT")
    @RunWith(JUnit3RunnerWithInners.class)
    public static class Arguments extends AbstractResolvedCallsTest {
        public void testAllFilesPresentInArguments() throws Exception {
            KotlinTestUtils.assertAllTestsPresentByMetadata(this.getClass(), new File("compiler/testData/resolvedCalls/arguments"), Pattern.compile("^(.+)\\.kt$"), TargetBackend.ANY, true);
        }

        @TestMetadata("compiler/testData/resolvedCalls/arguments/functionLiterals")
        @TestDataPath("$PROJECT_ROOT")
        @RunWith(JUnit3RunnerWithInners.class)
        public static class FunctionLiterals extends AbstractResolvedCallsTest {
            public void testAllFilesPresentInFunctionLiterals() throws Exception {
                KotlinTestUtils.assertAllTestsPresentByMetadata(this.getClass(), new File("compiler/testData/resolvedCalls/arguments/functionLiterals"), Pattern.compile("^(.+)\\.kt$"), TargetBackend.ANY, true);
            }

            @TestMetadata("chainedLambdas.kt")
            public void testChainedLambdas() throws Exception {
                String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/resolvedCalls/arguments/functionLiterals/chainedLambdas.kt");
                doTest(fileName);
            }

            @TestMetadata("notInferredLambdaReturnType.kt")
            public void testNotInferredLambdaReturnType() throws Exception {
                String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/resolvedCalls/arguments/functionLiterals/notInferredLambdaReturnType.kt");
                doTest(fileName);
            }

            @TestMetadata("notInferredLambdaType.kt")
            public void testNotInferredLambdaType() throws Exception {
                String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/resolvedCalls/arguments/functionLiterals/notInferredLambdaType.kt");
                doTest(fileName);
            }

            @TestMetadata("simpleGenericLambda.kt")
            public void testSimpleGenericLambda() throws Exception {
                String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/resolvedCalls/arguments/functionLiterals/simpleGenericLambda.kt");
                doTest(fileName);
            }

            @TestMetadata("simpleLambda.kt")
            public void testSimpleLambda() throws Exception {
                String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/resolvedCalls/arguments/functionLiterals/simpleLambda.kt");
                doTest(fileName);
            }

            @TestMetadata("unmappedLambda.kt")
            public void testUnmappedLambda() throws Exception {
                String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/resolvedCalls/arguments/functionLiterals/unmappedLambda.kt");
                doTest(fileName);
            }
        }

        @TestMetadata("compiler/testData/resolvedCalls/arguments/genericCalls")
        @TestDataPath("$PROJECT_ROOT")
        @RunWith(JUnit3RunnerWithInners.class)
        public static class GenericCalls extends AbstractResolvedCallsTest {
            public void testAllFilesPresentInGenericCalls() throws Exception {
                KotlinTestUtils.assertAllTestsPresentByMetadata(this.getClass(), new File("compiler/testData/resolvedCalls/arguments/genericCalls"), Pattern.compile("^(.+)\\.kt$"), TargetBackend.ANY, true);
            }

            @TestMetadata("inferredParameter.kt")
            public void testInferredParameter() throws Exception {
                String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/resolvedCalls/arguments/genericCalls/inferredParameter.kt");
                doTest(fileName);
            }

            @TestMetadata("simpleGeneric.kt")
            public void testSimpleGeneric() throws Exception {
                String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/resolvedCalls/arguments/genericCalls/simpleGeneric.kt");
                doTest(fileName);
            }

            @TestMetadata("uninferredParameter.kt")
            public void testUninferredParameter() throws Exception {
                String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/resolvedCalls/arguments/genericCalls/uninferredParameter.kt");
                doTest(fileName);
            }

            @TestMetadata("uninferredParameterTypeMismatch.kt")
            public void testUninferredParameterTypeMismatch() throws Exception {
                String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/resolvedCalls/arguments/genericCalls/uninferredParameterTypeMismatch.kt");
                doTest(fileName);
            }
        }

        @TestMetadata("compiler/testData/resolvedCalls/arguments/namedArguments")
        @TestDataPath("$PROJECT_ROOT")
        @RunWith(JUnit3RunnerWithInners.class)
        public static class NamedArguments extends AbstractResolvedCallsTest {
            public void testAllFilesPresentInNamedArguments() throws Exception {
                KotlinTestUtils.assertAllTestsPresentByMetadata(this.getClass(), new File("compiler/testData/resolvedCalls/arguments/namedArguments"), Pattern.compile("^(.+)\\.kt$"), TargetBackend.ANY, true);
            }

            @TestMetadata("positionedAfterNamed.kt")
            public void testPositionedAfterNamed() throws Exception {
                String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/resolvedCalls/arguments/namedArguments/positionedAfterNamed.kt");
                doTest(fileName);
            }

            @TestMetadata("shiftedArgsMatch.kt")
            public void testShiftedArgsMatch() throws Exception {
                String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/resolvedCalls/arguments/namedArguments/shiftedArgsMatch.kt");
                doTest(fileName);
            }
        }

        @TestMetadata("compiler/testData/resolvedCalls/arguments/oneArgument")
        @TestDataPath("$PROJECT_ROOT")
        @RunWith(JUnit3RunnerWithInners.class)
        public static class OneArgument extends AbstractResolvedCallsTest {
            public void testAllFilesPresentInOneArgument() throws Exception {
                KotlinTestUtils.assertAllTestsPresentByMetadata(this.getClass(), new File("compiler/testData/resolvedCalls/arguments/oneArgument"), Pattern.compile("^(.+)\\.kt$"), TargetBackend.ANY, true);
            }

            @TestMetadata("argumentHasNoType.kt")
            public void testArgumentHasNoType() throws Exception {
                String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/resolvedCalls/arguments/oneArgument/argumentHasNoType.kt");
                doTest(fileName);
            }

            @TestMetadata("simpleMatch.kt")
            public void testSimpleMatch() throws Exception {
                String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/resolvedCalls/arguments/oneArgument/simpleMatch.kt");
                doTest(fileName);
            }

            @TestMetadata("typeMismatch.kt")
            public void testTypeMismatch() throws Exception {
                String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/resolvedCalls/arguments/oneArgument/typeMismatch.kt");
                doTest(fileName);
            }

            @TestMetadata("unmappedArgument.kt")
            public void testUnmappedArgument() throws Exception {
                String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/resolvedCalls/arguments/oneArgument/unmappedArgument.kt");
                doTest(fileName);
            }
        }

        @TestMetadata("compiler/testData/resolvedCalls/arguments/realExamples")
        @TestDataPath("$PROJECT_ROOT")
        @RunWith(JUnit3RunnerWithInners.class)
        public static class RealExamples extends AbstractResolvedCallsTest {
            public void testAllFilesPresentInRealExamples() throws Exception {
                KotlinTestUtils.assertAllTestsPresentByMetadata(this.getClass(), new File("compiler/testData/resolvedCalls/arguments/realExamples"), Pattern.compile("^(.+)\\.kt$"), TargetBackend.ANY, true);
            }

            @TestMetadata("emptyList.kt")
            public void testEmptyList() throws Exception {
                String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/resolvedCalls/arguments/realExamples/emptyList.kt");
                doTest(fileName);
            }

            @TestMetadata("emptyMutableList.kt")
            public void testEmptyMutableList() throws Exception {
                String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/resolvedCalls/arguments/realExamples/emptyMutableList.kt");
                doTest(fileName);
            }
        }

        @TestMetadata("compiler/testData/resolvedCalls/arguments/severalCandidates")
        @TestDataPath("$PROJECT_ROOT")
        @RunWith(JUnit3RunnerWithInners.class)
        public static class SeveralCandidates extends AbstractResolvedCallsTest {
            public void testAllFilesPresentInSeveralCandidates() throws Exception {
                KotlinTestUtils.assertAllTestsPresentByMetadata(this.getClass(), new File("compiler/testData/resolvedCalls/arguments/severalCandidates"), Pattern.compile("^(.+)\\.kt$"), TargetBackend.ANY, true);
            }

            @TestMetadata("mostSpecific.kt")
            public void testMostSpecific() throws Exception {
                String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/resolvedCalls/arguments/severalCandidates/mostSpecific.kt");
                doTest(fileName);
            }
        }
    }

    @TestMetadata("compiler/testData/resolvedCalls/differentCallElements")
    @TestDataPath("$PROJECT_ROOT")
    @RunWith(JUnit3RunnerWithInners.class)
    public static class DifferentCallElements extends AbstractResolvedCallsTest {
        public void testAllFilesPresentInDifferentCallElements() throws Exception {
            KotlinTestUtils.assertAllTestsPresentByMetadata(this.getClass(), new File("compiler/testData/resolvedCalls/differentCallElements"), Pattern.compile("^(.+)\\.kt$"), TargetBackend.ANY, true);
        }

        @TestMetadata("annotationCall.kt")
        public void testAnnotationCall() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/resolvedCalls/differentCallElements/annotationCall.kt");
            doTest(fileName);
        }

        @TestMetadata("delegatorToSuperCall.kt")
        public void testDelegatorToSuperCall() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/resolvedCalls/differentCallElements/delegatorToSuperCall.kt");
            doTest(fileName);
        }

        @TestMetadata("simpleArrayAccess.kt")
        public void testSimpleArrayAccess() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/resolvedCalls/differentCallElements/simpleArrayAccess.kt");
            doTest(fileName);
        }
    }

    @TestMetadata("compiler/testData/resolvedCalls/dynamic")
    @TestDataPath("$PROJECT_ROOT")
    @RunWith(JUnit3RunnerWithInners.class)
    public static class Dynamic extends AbstractResolvedCallsTest {
        public void testAllFilesPresentInDynamic() throws Exception {
            KotlinTestUtils.assertAllTestsPresentByMetadata(this.getClass(), new File("compiler/testData/resolvedCalls/dynamic"), Pattern.compile("^(.+)\\.kt$"), TargetBackend.ANY, true);
        }

        @TestMetadata("explicitReceiverIsDispatchReceiver.kt")
        public void testExplicitReceiverIsDispatchReceiver() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/resolvedCalls/dynamic/explicitReceiverIsDispatchReceiver.kt");
            doTest(fileName);
        }

        @TestMetadata("explicitReceiverIsExtensionReceiver.kt")
        public void testExplicitReceiverIsExtensionReceiver() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/resolvedCalls/dynamic/explicitReceiverIsExtensionReceiver.kt");
            doTest(fileName);
        }

        @TestMetadata("hasBothDispatchAndExtensionReceivers.kt")
        public void testHasBothDispatchAndExtensionReceivers() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/resolvedCalls/dynamic/hasBothDispatchAndExtensionReceivers.kt");
            doTest(fileName);
        }

        @TestMetadata("hasBothDispatchAndExtensionReceiversWithoutExplicitReceiver.kt")
        public void testHasBothDispatchAndExtensionReceiversWithoutExplicitReceiver() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/resolvedCalls/dynamic/hasBothDispatchAndExtensionReceiversWithoutExplicitReceiver.kt");
            doTest(fileName);
        }

        @TestMetadata("implicitReceiverIsDispatchReceiver.kt")
        public void testImplicitReceiverIsDispatchReceiver() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/resolvedCalls/dynamic/implicitReceiverIsDispatchReceiver.kt");
            doTest(fileName);
        }

        @TestMetadata("implicitReceiverIsExtensionReceiver.kt")
        public void testImplicitReceiverIsExtensionReceiver() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/resolvedCalls/dynamic/implicitReceiverIsExtensionReceiver.kt");
            doTest(fileName);
        }
    }

    @TestMetadata("compiler/testData/resolvedCalls/functionTypes")
    @TestDataPath("$PROJECT_ROOT")
    @RunWith(JUnit3RunnerWithInners.class)
    public static class FunctionTypes extends AbstractResolvedCallsTest {
        public void testAllFilesPresentInFunctionTypes() throws Exception {
            KotlinTestUtils.assertAllTestsPresentByMetadata(this.getClass(), new File("compiler/testData/resolvedCalls/functionTypes"), Pattern.compile("^(.+)\\.kt$"), TargetBackend.ANY, true);
        }

        @TestMetadata("invokeForExtensionFunctionType.kt")
        public void testInvokeForExtensionFunctionType() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/resolvedCalls/functionTypes/invokeForExtensionFunctionType.kt");
            doTest(fileName);
        }

        @TestMetadata("invokeForFunctionType.kt")
        public void testInvokeForFunctionType() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/resolvedCalls/functionTypes/invokeForFunctionType.kt");
            doTest(fileName);
        }

        @TestMetadata("valOfExtensionFunctionType.kt")
        public void testValOfExtensionFunctionType() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/resolvedCalls/functionTypes/valOfExtensionFunctionType.kt");
            doTest(fileName);
        }

        @TestMetadata("valOfExtensionFunctionTypeInvoke.kt")
        public void testValOfExtensionFunctionTypeInvoke() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/resolvedCalls/functionTypes/valOfExtensionFunctionTypeInvoke.kt");
            doTest(fileName);
        }

        @TestMetadata("valOfFunctionType.kt")
        public void testValOfFunctionType() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/resolvedCalls/functionTypes/valOfFunctionType.kt");
            doTest(fileName);
        }

        @TestMetadata("valOfFunctionTypeInvoke.kt")
        public void testValOfFunctionTypeInvoke() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/resolvedCalls/functionTypes/valOfFunctionTypeInvoke.kt");
            doTest(fileName);
        }
    }

    @TestMetadata("compiler/testData/resolvedCalls/invoke")
    @TestDataPath("$PROJECT_ROOT")
    @RunWith(JUnit3RunnerWithInners.class)
    public static class Invoke extends AbstractResolvedCallsTest {
        public void testAllFilesPresentInInvoke() throws Exception {
            KotlinTestUtils.assertAllTestsPresentByMetadata(this.getClass(), new File("compiler/testData/resolvedCalls/invoke"), Pattern.compile("^(.+)\\.kt$"), TargetBackend.ANY, true);
        }

        @TestMetadata("bothReceivers.kt")
        public void testBothReceivers() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/resolvedCalls/invoke/bothReceivers.kt");
            doTest(fileName);
        }

        @TestMetadata("dispatchReceiverAsReceiverForInvoke.kt")
        public void testDispatchReceiverAsReceiverForInvoke() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/resolvedCalls/invoke/dispatchReceiverAsReceiverForInvoke.kt");
            doTest(fileName);
        }

        @TestMetadata("extensionReceiverAsReceiverForInvoke.kt")
        public void testExtensionReceiverAsReceiverForInvoke() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/resolvedCalls/invoke/extensionReceiverAsReceiverForInvoke.kt");
            doTest(fileName);
        }

        @TestMetadata("implicitReceiverForInvoke.kt")
        public void testImplicitReceiverForInvoke() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/resolvedCalls/invoke/implicitReceiverForInvoke.kt");
            doTest(fileName);
        }

        @TestMetadata("invokeOnClassObject1.kt")
        public void testInvokeOnClassObject1() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/resolvedCalls/invoke/invokeOnClassObject1.kt");
            doTest(fileName);
        }

        @TestMetadata("invokeOnClassObject2.kt")
        public void testInvokeOnClassObject2() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/resolvedCalls/invoke/invokeOnClassObject2.kt");
            doTest(fileName);
        }

        @TestMetadata("invokeOnEnumEntry1.kt")
        public void testInvokeOnEnumEntry1() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/resolvedCalls/invoke/invokeOnEnumEntry1.kt");
            doTest(fileName);
        }

        @TestMetadata("invokeOnEnumEntry2.kt")
        public void testInvokeOnEnumEntry2() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/resolvedCalls/invoke/invokeOnEnumEntry2.kt");
            doTest(fileName);
        }

        @TestMetadata("invokeOnObject1.kt")
        public void testInvokeOnObject1() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/resolvedCalls/invoke/invokeOnObject1.kt");
            doTest(fileName);
        }

        @TestMetadata("invokeOnObject2.kt")
        public void testInvokeOnObject2() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/resolvedCalls/invoke/invokeOnObject2.kt");
            doTest(fileName);
        }
    }

    @TestMetadata("compiler/testData/resolvedCalls/objectsAndClassObjects")
    @TestDataPath("$PROJECT_ROOT")
    @RunWith(JUnit3RunnerWithInners.class)
    public static class ObjectsAndClassObjects extends AbstractResolvedCallsTest {
        public void testAllFilesPresentInObjectsAndClassObjects() throws Exception {
            KotlinTestUtils.assertAllTestsPresentByMetadata(this.getClass(), new File("compiler/testData/resolvedCalls/objectsAndClassObjects"), Pattern.compile("^(.+)\\.kt$"), TargetBackend.ANY, true);
        }

        @TestMetadata("classObject.kt")
        public void testClassObject() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/resolvedCalls/objectsAndClassObjects/classObject.kt");
            doTest(fileName);
        }

        @TestMetadata("kt5308IntRangeConstant.kt")
        public void testKt5308IntRangeConstant() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/resolvedCalls/objectsAndClassObjects/kt5308IntRangeConstant.kt");
            doTest(fileName);
        }

        @TestMetadata("object.kt")
        public void testObject() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/resolvedCalls/objectsAndClassObjects/object.kt");
            doTest(fileName);
        }
    }

    @TestMetadata("compiler/testData/resolvedCalls/realExamples")
    @TestDataPath("$PROJECT_ROOT")
    @RunWith(JUnit3RunnerWithInners.class)
    public static class RealExamples extends AbstractResolvedCallsTest {
        public void testAllFilesPresentInRealExamples() throws Exception {
            KotlinTestUtils.assertAllTestsPresentByMetadata(this.getClass(), new File("compiler/testData/resolvedCalls/realExamples"), Pattern.compile("^(.+)\\.kt$"), TargetBackend.ANY, true);
        }

        @TestMetadata("stringPlusInBuilders.kt")
        public void testStringPlusInBuilders() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/resolvedCalls/realExamples/stringPlusInBuilders.kt");
            doTest(fileName);
        }
    }

    @TestMetadata("compiler/testData/resolvedCalls/resolve")
    @TestDataPath("$PROJECT_ROOT")
    @RunWith(JUnit3RunnerWithInners.class)
    public static class Resolve extends AbstractResolvedCallsTest {
        public void testAllFilesPresentInResolve() throws Exception {
            KotlinTestUtils.assertAllTestsPresentByMetadata(this.getClass(), new File("compiler/testData/resolvedCalls/resolve"), Pattern.compile("^(.+)\\.kt$"), TargetBackend.ANY, true);
        }

        @TestMetadata("mostSpecificUninferredParam.kt")
        public void testMostSpecificUninferredParam() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/resolvedCalls/resolve/mostSpecificUninferredParam.kt");
            doTest(fileName);
        }

        @TestMetadata("mostSpecificWithLambda.kt")
        public void testMostSpecificWithLambda() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/resolvedCalls/resolve/mostSpecificWithLambda.kt");
            doTest(fileName);
        }
    }

    @TestMetadata("compiler/testData/resolvedCalls/secondaryConstructors")
    @TestDataPath("$PROJECT_ROOT")
    @RunWith(JUnit3RunnerWithInners.class)
    public static class SecondaryConstructors extends AbstractResolvedCallsTest {
        public void testAllFilesPresentInSecondaryConstructors() throws Exception {
            KotlinTestUtils.assertAllTestsPresentByMetadata(this.getClass(), new File("compiler/testData/resolvedCalls/secondaryConstructors"), Pattern.compile("^(.+)\\.kt$"), TargetBackend.ANY, true);
        }

        @TestMetadata("classWithGenerics.kt")
        public void testClassWithGenerics() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/resolvedCalls/secondaryConstructors/classWithGenerics.kt");
            doTest(fileName);
        }

        @TestMetadata("classWithGenerics2.kt")
        public void testClassWithGenerics2() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/resolvedCalls/secondaryConstructors/classWithGenerics2.kt");
            doTest(fileName);
        }

        @TestMetadata("classWithGenerics3.kt")
        public void testClassWithGenerics3() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/resolvedCalls/secondaryConstructors/classWithGenerics3.kt");
            doTest(fileName);
        }

        @TestMetadata("explicitPrimaryArgs.kt")
        public void testExplicitPrimaryArgs() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/resolvedCalls/secondaryConstructors/explicitPrimaryArgs.kt");
            doTest(fileName);
        }

        @TestMetadata("explicitPrimaryCallSecondary.kt")
        public void testExplicitPrimaryCallSecondary() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/resolvedCalls/secondaryConstructors/explicitPrimaryCallSecondary.kt");
            doTest(fileName);
        }

        @TestMetadata("explicitPrimaryNoArgs.kt")
        public void testExplicitPrimaryNoArgs() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/resolvedCalls/secondaryConstructors/explicitPrimaryNoArgs.kt");
            doTest(fileName);
        }

        @TestMetadata("implicitPrimary.kt")
        public void testImplicitPrimary() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/resolvedCalls/secondaryConstructors/implicitPrimary.kt");
            doTest(fileName);
        }

        @TestMetadata("overload1.kt")
        public void testOverload1() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/resolvedCalls/secondaryConstructors/overload1.kt");
            doTest(fileName);
        }

        @TestMetadata("overload2.kt")
        public void testOverload2() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/resolvedCalls/secondaryConstructors/overload2.kt");
            doTest(fileName);
        }

        @TestMetadata("overload3.kt")
        public void testOverload3() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/resolvedCalls/secondaryConstructors/overload3.kt");
            doTest(fileName);
        }

        @TestMetadata("overloadDefault.kt")
        public void testOverloadDefault() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/resolvedCalls/secondaryConstructors/overloadDefault.kt");
            doTest(fileName);
        }

        @TestMetadata("overloadNamed.kt")
        public void testOverloadNamed() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/resolvedCalls/secondaryConstructors/overloadNamed.kt");
            doTest(fileName);
        }

        @TestMetadata("simple.kt")
        public void testSimple() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/resolvedCalls/secondaryConstructors/simple.kt");
            doTest(fileName);
        }

        @TestMetadata("varargs.kt")
        public void testVarargs() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/resolvedCalls/secondaryConstructors/varargs.kt");
            doTest(fileName);
        }
    }

    @TestMetadata("compiler/testData/resolvedCalls/thisOrSuper")
    @TestDataPath("$PROJECT_ROOT")
    @RunWith(JUnit3RunnerWithInners.class)
    public static class ThisOrSuper extends AbstractResolvedCallsTest {
        public void testAllFilesPresentInThisOrSuper() throws Exception {
            KotlinTestUtils.assertAllTestsPresentByMetadata(this.getClass(), new File("compiler/testData/resolvedCalls/thisOrSuper"), Pattern.compile("^(.+)\\.kt$"), TargetBackend.ANY, true);
        }

        @TestMetadata("labeledSuper.kt")
        public void testLabeledSuper() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/resolvedCalls/thisOrSuper/labeledSuper.kt");
            doTest(fileName);
        }

        @TestMetadata("labeledThis.kt")
        public void testLabeledThis() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/resolvedCalls/thisOrSuper/labeledThis.kt");
            doTest(fileName);
        }

        @TestMetadata("simpleSuper.kt")
        public void testSimpleSuper() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/resolvedCalls/thisOrSuper/simpleSuper.kt");
            doTest(fileName);
        }

        @TestMetadata("simpleThis.kt")
        public void testSimpleThis() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/resolvedCalls/thisOrSuper/simpleThis.kt");
            doTest(fileName);
        }

        @TestMetadata("thisInExtensionFunction.kt")
        public void testThisInExtensionFunction() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/resolvedCalls/thisOrSuper/thisInExtensionFunction.kt");
            doTest(fileName);
        }
    }
}
