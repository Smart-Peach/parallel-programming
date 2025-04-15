package org.nsu.syspro.parprog.solution;

import org.nsu.syspro.parprog.UserThread;
import org.nsu.syspro.parprog.external.*;

import java.util.HashMap;
import java.util.Map;

public class SolutionThread extends UserThread {

    final static Map<MethodID, CompiledMethod> compiledMethods1 = new HashMap<>();
    final static Map<MethodID, CompiledMethod> compiledMethods2 = new HashMap<>();
    final Map<MethodID, Integer> numberOfMethodCalls = new HashMap<>();
    final Map<MethodID, Boolean> flags = new HashMap<>();

    public SolutionThread(int compilationThreadBound, ExecutionEngine exec, CompilationEngine compiler, Runnable r) {
        super(compilationThreadBound, exec, compiler, r);
    }


    @Override
    public ExecutionResult executeMethod(MethodID id) {
        synchronized (numberOfMethodCalls) {
            numberOfMethodCalls.compute(id, (key, val) -> (val == null) ? 1 : val + 1);
        }

        synchronized (compiledMethods2) {
            synchronized (compiledMethods1) {
                if (!isHot(id) && !isCompiled(id)) {
                    return exec.interpret(id);
                }

                CompiledMethod compiledMethod;
                // If method is already hot enough to compile it
                if (isHot(id) && !isCompiled(id)) {
//                    numberOfMethodCalls.put(id, 0);
                    System.out.println(id.id() + " BEFORE L1 compilation");
                    compiledMethods1.put(id, compiler.compile_l1(id));
                    compiledMethod = compiledMethods1.get(id);
                    System.out.println(id.id() + " is L1 compiled");
                }
                // If method is extra hot then recompile with level 2
                else if (isBoiling(id) && isCompiled1(id)) {
                    compiledMethods1.remove(id);
                    System.out.println(id.id() + " BEFORE L2 compilation");
                    compiledMethods2.put(id, compiler.compile_l2(id));
                    compiledMethod = compiledMethods2.get(id);
                    System.out.println(id.id() + " is L2 compiled");
                    // If method is already compiled, and we do not have requirement to change it
                } else {
                    compiledMethod = getCompiledMethod(id);
                }
                assert (compiledMethod != null);
                return this.exec.execute(compiledMethod);
            }
        }

    }

    private boolean isCompiled1(MethodID id) {
        synchronized (compiledMethods1) {
            return compiledMethods1.containsKey(id);
        }
    }

    private boolean isCompiled2(MethodID id) {
        synchronized (compiledMethods2) {
            return compiledMethods2.containsKey(id);
        }
    }

    private boolean isCompiled(MethodID id) {
        return isCompiled1(id) || isCompiled2(id);
    }

    private boolean isHot(MethodID id) {
        synchronized (numberOfMethodCalls) {
            return numberOfMethodCalls.get(id) > 9999;
        }
    }

    private boolean isBoiling(MethodID id) {
        synchronized (numberOfMethodCalls) {
            return numberOfMethodCalls.get(id) > 99999;
        }
    }

    private CompiledMethod getCompiledMethod(MethodID id) {
        if (isCompiled1(id)) {
            return compiledMethods1.get(id);
        }
        if (isCompiled2(id)) {
            return compiledMethods2.get(id);
        }
        return null;
    }
}