package org.nsu.syspro.parprog.solution;

import org.nsu.syspro.parprog.UserThread;
import org.nsu.syspro.parprog.external.*;

import java.util.HashMap;
import java.util.Map;

public class SolutionThread extends UserThread {
    /**
     * Cache of compiled MethodID data, consists of MethodID which were compiled with level 1.
     */
    final private static Map<MethodID, CompiledMethod> compiledMethods1 = new HashMap<>();
    /**
     * Cache of compiled MethodID data, consists of MethodID which were compiled with level 2.
     */
    final private static Map<MethodID, CompiledMethod> compiledMethods2 = new HashMap<>();
    /**
     * Auxiliary counter. It is a counter which shows how many times each MethodID was executed.
     */
    final private Map<MethodID, Integer> numberOfMethodCalls = new HashMap<>();

    public SolutionThread(int compilationThreadBound, ExecutionEngine exec, CompilationEngine compiler, Runnable r) {
        super(compilationThreadBound, exec, compiler, r);
    }


    @Override
    public ExecutionResult executeMethod(MethodID id) {
        synchronized (numberOfMethodCalls) {
            numberOfMethodCalls.compute(id, (key, val) -> (val == null) ? 1 : val + 1);
        }
        // If method isn't hot enough then we just interpret it
        if (!isHot(id) && !isCompiled(id)) {
            return exec.interpret(id);
        }
        Thread compilation2 = new Thread(() -> compiledMethods2.put(id, compiler.compile_l2(id)));
        CompiledMethod compiledMethod;
        synchronized (compiledMethods2) {
            synchronized (compiledMethods1) {
                // If method is already hot enough to compile it with level 1
                if (isHot(id) && !isCompiled(id)) {
                    compiledMethods1.put(id, compiler.compile_l1(id));
                    compiledMethod = compiledMethods1.get(id);
                }
                // If method is extra hot then recompile with level 2
                else if (isExtraHot(id) && !isCompiled2(id)) {
                    compilation2.start();
                    compiledMethods1.remove(id);
                    try {
                        compilation2.join();
                    } catch (InterruptedException e) {
                        System.err.println("Interruption has occurred!");
                    }
                    compiledMethod = compiledMethods2.get(id);
                } else { // If method is already compiled, and we do not have requirement to change compilation level, then use already compiled data
                    compiledMethod = getCompiledMethod(id);
                }
                assert (compiledMethod != null);
                return this.exec.execute(compiledMethod);
            }
        }

    }

    /**
     * Checks if 'id' was compiled with level 1 JIT by finding it in cache
     *
     * @param id is MethodID
     * @return true - if 'id' is compiled with level 1 JIT, otherwise - false
     */
    private boolean isCompiled1(MethodID id) {
        synchronized (compiledMethods1) {
            return compiledMethods1.containsKey(id);
        }
    }

    /**
     * Checks if 'id' was compiled with level 2 JIT by finding it in cache
     *
     * @param id is MethodId
     * @return true - if 'id' is compiled with level 2 JIT, otherwise - false
     */
    private boolean isCompiled2(MethodID id) {
        synchronized (compiledMethods2) {
            return compiledMethods2.containsKey(id);
        }
    }

    /**
     * Checks if 'id' was compiled with any level JIT
     *
     * @param id is MethodID
     * @return true - if 'id' is compiled with any level JIT, otherwise - false
     */
    private boolean isCompiled(MethodID id) {
        return isCompiled1(id) || isCompiled2(id);
    }

    /**
     * Checks, if MethodID was {@link #executeMethod(MethodID)} more than 9999 times
     *
     * @param id is MethodID
     * @return true - if 'id' has executed more than 9999 times, otherwise - false
     */
    private boolean isHot(MethodID id) {
        synchronized (numberOfMethodCalls) {
            return numberOfMethodCalls.get(id) > 9999;
        }
    }

    /**
     * Checks, if MethodID was {@link #executeMethod(MethodID)} more than 99999 times
     *
     * @param id is MethodID
     * @return true - if 'id' has executed more than 99999 times, otherwise - false
     */
    private boolean isExtraHot(MethodID id) {
        synchronized (numberOfMethodCalls) {
            return numberOfMethodCalls.get(id) > 99999;
        }
    }

    /**
     * Checks if 'id' was compiled before and returns compiled data of this method
     *
     * @param id is MethodID
     * @return compiled data of MethodID. If MethodID has not been compiled before, returns null
     */
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