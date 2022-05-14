/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.test;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestResult;
import junit.framework.TestSuite;
import org.junit.internal.MethodSorter;
import org.junit.internal.runners.JUnit38ClassRunner;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.manipulation.*;
import org.junit.runner.notification.RunNotifier;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * This runner runs class with all inners test classes, but monitors situation when those classes are planned to be executed
 * with IDEA package test runner.
 */
public class JUnit3RunnerWithInners extends Runner implements Filterable, Sortable {
    private static final Set<Class> requestedRunners = new HashSet<Class>();

    private JUnit38ClassRunner delegateRunner;
    private final Class<?> klass;
    private boolean isFakeTest = false;

    private static class FakeEmptyClassTest implements Test {
        private final Class<?> klass;

        public FakeEmptyClassTest(Class<?> klass) {
            this.klass = klass;
        }

        @Override
        public int countTestCases() {
            return 0;
        }

        @Override
        public void run(TestResult result) {
            result.startTest(this);
            result.endTest(this);
        }

        public Class<?> getTestClass() {
            return klass;
        }

        @Override
        public String toString() {
            return "Empty class with inners";
        }
    }

    public JUnit3RunnerWithInners(Class<?> klass) {
        this.klass = klass;
        requestedRunners.add(klass);
    }

    @Override
    public void run(RunNotifier notifier) {
        initialize();
        delegateRunner.run(notifier);
    }

    @Override
    public Description getDescription() {
        initialize();
        return isFakeTest ? Description.EMPTY : delegateRunner.getDescription();
    }

    @Override
    public void filter(Filter filter) throws NoTestsRemainException {
        delegateRunner = new JUnit38ClassRunner(klass);
        delegateRunner.filter(filter);
    }

    @Override
    public void sort(Sorter sorter) {
        initialize();
        delegateRunner.sort(sorter);
    }

    protected void initialize() {
        if (delegateRunner != null) return;
        delegateRunner = new JUnit38ClassRunner(getCollectedTests());
    }

    protected Test getCollectedTests() {
        List<Class> innerClasses = collectDeclaredClasses(klass, false);
        Set<Class> unprocessedInnerClasses = unprocessedClasses(innerClasses);

        if (unprocessedInnerClasses.isEmpty()) {
            if (!innerClasses.isEmpty() && !hasTestMethods(klass)) {
                isFakeTest = true;
                return new FakeEmptyClassTest(klass);
            }
            else {
                return new TestSuite(klass.asSubclass(TestCase.class));
            }
        }
        else if (unprocessedInnerClasses.size() == innerClasses.size()) {
            return createTreeTestSuite(klass);
        }
        else {
            return new TestSuite(klass.asSubclass(TestCase.class));
        }
    }

    private static Test createTreeTestSuite(Class root) {
        Set<Class> classes = new LinkedHashSet<Class>(collectDeclaredClasses(root, true));
        Map<Class, TestSuite> classSuites = new HashMap<Class, TestSuite>();

        for (Class aClass : classes) {
            classSuites.put(aClass, hasTestMethods(aClass) ? new TestSuite(aClass) : new TestSuite(aClass.getCanonicalName()));
        }

        for (Class aClass : classes) {
            if (aClass.getEnclosingClass() != null && classes.contains(aClass.getEnclosingClass())) {
                classSuites.get(aClass.getEnclosingClass()).addTest(classSuites.get(aClass));
            }
        }

        return classSuites.get(root);
    }

    private static Set<Class> unprocessedClasses(Collection<Class> classes) {
        Set<Class> result = new LinkedHashSet<Class>();
        for (Class aClass : classes) {
            if (!requestedRunners.contains(aClass)) {
                result.add(aClass);
            }
        }

        return result;
    }

    private static List<Class> collectDeclaredClasses(Class klass, boolean withItself) {
        List<Class> result = new ArrayList<Class>();
        if (withItself) {
            result.add(klass);
        }

        for (Class aClass : klass.getDeclaredClasses()) {
            result.addAll(collectDeclaredClasses(aClass, true));
        }

        return result;
    }

    private static boolean hasTestMethods(Class klass) {
        for (Class currentClass = klass; Test.class.isAssignableFrom(currentClass); currentClass = currentClass.getSuperclass()) {
            for (Method each : MethodSorter.getDeclaredMethods(currentClass)) {
                if (isTestMethod(each)) return true;
            }
        }

        return false;
    }

    private static boolean isTestMethod(Method method) {
        return method.getParameterTypes().length == 0 &&
               method.getName().startsWith("test") &&
               method.getReturnType().equals(Void.TYPE) &&
               Modifier.isPublic(method.getModifiers());
    }
}