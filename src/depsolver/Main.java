package depsolver;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import java.io.*;
import java.util.*;



public class Main {
    private static boolean satisfies = false;
    private static List<String> commandsArray = new ArrayList<>();
    private static HashSet<List<String>> visitedSetHash = new HashSet<>();
    private static HashMap<String, Integer> solutionHash = new HashMap<>();
    private static HashMap<String, Integer> maxConflictHash = new HashMap<>();

    /**
     * Find the optimal solution for a dependency issue
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        TypeReference<List<Package>> repoType = new TypeReference<List<Package>>() {    };
        List<Package> repo = JSON.parseObject(readFile(args[0]), repoType);
        TypeReference<List<String>> strListType = new TypeReference<List<String>>() {   };
        List<String> initial = JSON.parseObject(readFile(args[1]), strListType);
        List<String> constraints = JSON.parseObject(readFile(args[2]), strListType);
        while (!(packetValid(initial, repo))) {
            initial = rmBadPackets(initial, repo);
        }
        search(initial, repo, initial, constraints);
        getANS();
    }

    /**
     * Helper function to reduce duplicate code
     * @return
     */
    private static String cmdStr()    {
        String cTemp = "";
        for (String T : commandsArray) {
            cTemp = cTemp + T;
            cTemp = cTemp + ",";
        }
        return cTemp;
    }

    /**
     * Search for a valid  solution
     * @param set
     * @param repo
     * @param initial
     * @param constraints
     */
    private static void search(List<String> set, List<Package> repo, List<String> initial, List<String> constraints) {
        int price;
        if (!packetValid(set, repo)) return;
        if (visitedSetHash.contains(set))  return;
        if (bottomoStatment(set, constraints)) {
            satisfies = true;
            price = price(repo);
            String cTemp = cmdStr();
            solutionHash.put(cTemp, price);
            return;
        }
        visitedSetHash.add(set);
        fixPack(set, repo, initial, constraints);
    }

    /**
     * Repair a faulty packet
     * @param set
     * @param repo
     * @param initial
     * @param constraints
     */
    private static void fixPack(List<String> set, List<Package> repo, List<String> initial, List<String> constraints)   {
        for (Package pack : repo) {
            if (initial.contains(pack.getName() + "=" + pack.getVersion())) {
                set.remove(pack.getName() + "=" + pack.getVersion());
                commandsArray.add("-" + pack.getName() + "=" + pack.getVersion());
                search(set, repo, initial, constraints);
                set.add(pack.getName() + "=" + pack.getVersion());
                commandsArray.remove("-" + pack.getName() + "=" + pack.getVersion());
            } else if (!(commandsArray.contains("-" + pack.getName() + "=" + pack.getVersion())) && !(set.contains(pack.getName() + "=" + pack.getVersion()))) {
                set.add(pack.getName() + "=" + pack.getVersion());
                commandsArray.add("+" + pack.getName() + "=" + pack.getVersion());
                search(set, repo, initial, constraints);
                set.remove(pack.getName() + "=" + pack.getVersion());
                commandsArray.remove("+" + pack.getName() + "=" + pack.getVersion());
            }
        }
    }

    /**
     * Checks is a packet is faulty
     * @param set
     * @param repo
     * @return a boolean to determine is a packet is valid or not
     */
    private static boolean packetValid(List<String> set, List<Package> repo) {
        for (Package pack : repo) {
            if (set.contains(pack.getName() + "=" + pack.getVersion())) {
                for (List<String> subsectionArray : pack.getDepends()) {
                    boolean fnd;
                    fnd = false;
                    for (String p : subsectionArray) {
                        String[] spli_t;
                        spli_t = packageSpli_t(p);
                        String compareOperator;
                        compareOperator = spli_t[2];
                        if (!fnd) {
                            for (String T : set) {
                                String[] spl_i_t;
                                spl_i_t = packageSpli_t(T);
                                if (spli_t[0].equals(spl_i_t[0])) {
                                    switch (compareOperator) {
                                        case "=" : if (p.equals(T)) fnd = true; break;
                                        case "<" : if (!spli_t[1].equals(spl_i_t[1]) && versionCompare(spli_t[1], spl_i_t[1])) fnd = true; break;
                                        case "<=": if (versionCompare(spli_t[1], spl_i_t[1])) fnd = true; break;
                                        case ">" : if (!spli_t[1].equals(spl_i_t[1]) && (versionCompare(spl_i_t[1], spli_t[1]))) fnd = true; break;
                                        case ">=": if (versionCompare(spl_i_t[1], spli_t[1])) fnd = true; break;
                                        default: fnd = true; break;
                                    }
                                }
                            }
                        }
                    }
                    if (!fnd)
                        return false;
                }

                boolean foundConflict;
                foundConflict = false;

                for (String T : pack.getConflicts()) {
                    String[] spli_tCons;
                    spli_tCons = packageSpli_t(T);
                    String compareOperator;
                    compareOperator = spli_tCons[2];
                    if (!foundConflict) {
                        for (String S : set) {
                            String[] split_S;
                            split_S = packageSpli_t(S);
                            if (spli_tCons[0].equals(split_S[0])) {
                                switch (compareOperator) {
                                    case "=": if (T.equals(S)) {
                                            foundConflict = true;
                                            Integer value;
                                            value = maxConflictHash.get(S);
                                            if (value == null) maxConflictHash.put(S, 1);
                                            else maxConflictHash.put(S, value++);
                                        }
                                        break;

                                    case "<": if (!(spli_tCons[1].equals(split_S[1])) && versionCompare(spli_tCons[1], split_S[1])) {
                                                foundConflict = true;
                                                Integer value;
                                                value = maxConflictHash.get(S);
                                                if (value == null) maxConflictHash.put(S, 1);
                                                else maxConflictHash.put(S, value++);
                                        }
                                        break;

                                    case "<=": if (versionCompare(spli_tCons[1], split_S[1])) {
                                            foundConflict = true;
                                            Integer value;
                                            value = maxConflictHash.get(S);

                                            if (value == null) maxConflictHash.put(S, 1);
                                            else maxConflictHash.put(S, value++);
                                        }
                                        break;

                                    case ">": if (!(spli_tCons[1].equals(split_S[1]) && versionCompare(split_S[1], spli_tCons[1]))) {
                                                foundConflict = true;
                                                Integer value;
                                                value = maxConflictHash.get(S);
                                                if (value == null) maxConflictHash.put(S, 1);
                                                else maxConflictHash.put(S, value++);
                                        }
                                        break;

                                    case ">=": if (versionCompare(split_S[1], spli_tCons[1])) {
                                            foundConflict = true;
                                            Integer value;
                                            value = maxConflictHash.get(S);
                                            if (value == null) maxConflictHash.put(S, 1);
                                            else maxConflictHash.put(S, value++);
                                        }
                                        break;

                                    default:
                                        foundConflict = true;
                                        Integer value;
                                        value = maxConflictHash.get(S);
                                        if (value == null) maxConflictHash.put(S, 1);
                                        else maxConflictHash.put(S, value++);
                                        break;

                                }
                            }
                        }
                    }
                }
                if (foundConflict) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Check if we are at the bottom of a dependancy tree
     * @param set
     * @param constraints
     * @return boolean of if we have reached the bottom of the dependency tree
     */
    private static boolean bottomoStatment(List<String> set, List<String> constraints) {
        for (String T : constraints) {
            String operator;
            operator = Character.toString(T.charAt(0));
            T = T.substring(1);
            String[] versionConsName = packageSpli_t(T);
            String nameCons = versionConsName[0];
            String versionCons = versionConsName[1];
            String compareOperator;
            compareOperator = versionConsName[2];
            boolean metConstraints;
            metConstraints = false;
            if (operator.equals("+")) {
                for (String pack : set) {
                    String[] split_P;
                    split_P = packageSpli_t(pack);
                    if (nameCons.equals(split_P[0])) {
                        switch (compareOperator) {
                            case "=": if (versionCons.equals(split_P[1])) metConstraints = true; break;
                            case "<": break;
                            case "<=": break;
                            case ">": break;
                            case ">=": break;
                            default: metConstraints = true; break;
                        }
                    }
                }
            }
            if (!metConstraints) return false;
            if (operator.equals("-")) {
                int i = 0;
                for (String pack : set) {
                    String[] split_P;
                    split_P = packageSpli_t(pack);
                    switch (compareOperator) {
                        case "=":
                            if (versionCons.equals(split_P[1]) && nameCons.equals(split_P[0])) i++; break;
                        case "<" : break;
                        case "<=": break;
                        case ">" : break;
                        case ">=": break;
                        default  : break;
                    }
                }
                if (i > 0) return false;
            }
        }
        return true;
    }

    /**
     * remove any pad packages
     * @param set
     * @param repo
     * @return a complete set
     */
    private static List<String> rmBadPackets(List<String> set, List<Package> repo) {
        List<String> rm = new ArrayList<>();
        int increment = 0;
        int limit;
        limit = set.size();
        for (Package pack : repo) {
            if (limit <= increment) break;
            boolean packageThrown = false;
            if (set.contains(pack.getName() + "=" + pack.getVersion())) {
                increment++;
                for (List<String> subsectionArray : pack.getDepends()) {
                    boolean fnd = false;
                    for (String p : subsectionArray) {
                        String[] spli_t;
                        spli_t = packageSpli_t(p);
                        String compareOperator;
                        compareOperator = spli_t[2];
                        if (!p.contains("=")) {
                            if (!fnd) {
                                for (String T : set) {
                                    String[] spl_i_t;
                                    spl_i_t = packageSpli_t(T);
                                    if (spli_t[0].equals(spl_i_t[0])) {
                                        switch (compareOperator) {
                                            case "=": if (p.equals(T)) fnd = true; break;
                                            case "<": if (!(spli_t[1].equals(spl_i_t[1])) && versionCompare(spli_t[1], spl_i_t[1])) fnd = true; break;
                                            case "<=": if (versionCompare(spli_t[1], spl_i_t[1])) fnd = true; break;
                                            case ">": if (!(spli_t[1].equals(spl_i_t[1])) && versionCompare(spl_i_t[1], spli_t[1])) fnd = true; break;
                                            case ">=": if (versionCompare(spl_i_t[1], spli_t[1])) fnd = true; break;
                                            default: fnd = true; break;
                                        }
                                    }
                                }
                            }
                        } else {
                            if (set.contains(p)) fnd = true;
                        }
                    }
                    if (!fnd) {
                        packageThrown = true;
                        set.remove(pack.getName() + "=" + pack.getVersion());
                        commandsArray.add("-" + pack.getName() + "=" + pack.getVersion());
                    }
                }
                if (!packageThrown) {
                    boolean foundConflict = false;
                    for (String T : pack.getConflicts()) {
                        String[] spli_tCons = packageSpli_t(T);
                        String compareOperator = spli_tCons[2];
                        if (!foundConflict) {
                            for (String S : set) {
                                String[] split_S = packageSpli_t(S);
                                if (spli_tCons[0].equals(split_S[0])) {
                                    switch (compareOperator) {
                                        case "=" : if (T.equals(S)) foundConflict = true; break;
                                        case "<" : if (!(spli_tCons[1].equals(split_S[1]) && (versionCompare(spli_tCons[1], split_S[1])))) foundConflict = true; break;
                                        case "<=": if (versionCompare(spli_tCons[1], split_S[1])) foundConflict = true; break;
                                        case ">" : if (!(spli_tCons[1].equals(split_S[1])) && (versionCompare(split_S[1], spli_tCons[1]))) foundConflict = true; break;
                                        case ">=": if (versionCompare(split_S[1], spli_tCons[1])) foundConflict = true; break;
                                        default  : foundConflict = true; break;
                                    }
                                }
                            }
                        }
                    }
                    if (foundConflict) {
                        rm.add(pack.getName() + "=" + pack.getVersion());
                        set.remove(pack.getName() + "=" + pack.getVersion());
                        commandsArray.add("-" + pack.getName() + "=" + pack.getVersion());
                    }
                }
            }
        }
        return set;
    }

    /**
     * decided which version of a of two strings is better
     * @param v1
     * @param v2
     * @return a boolean of ewhich of the two versions are better
     */
    private static boolean versionCompare(String v1, String v2) {
        String vT1 = v1.replace("0", "");
        String vT2 = v2.replace("0", "");
        if (!vT1.isEmpty()) v1 = vT1;
        if (!vT2.isEmpty()) v2 = vT2;
        List<String> v1Array = new ArrayList(Arrays.asList(v1.split("\\.")));
        List<String> v2Array = new ArrayList(Arrays.asList(v2.split("\\.")));
        while (v1Array.size() != v2Array.size()) {
            if (v1Array.size() > v2Array.size()) v2Array.add("0");
            else v1Array.add("0");
        }
        for (int i = 0; i < v1Array.size(); i++) {
            if (Integer.parseInt(v1Array.get(i)) > Integer.parseInt(v2Array.get(i))) return true;
            if (Integer.parseInt(v2Array.get(i)) > Integer.parseInt(v1Array.get(i))) return false;
        }
        return true;
    }

    /**
     * Xreate an array containing a possible solution.
     * @param S
     * @return an acceptable array solution
     */
    private static String[] packageSpli_t(String S) {
        String[] versionConsName = new String[2];
        String nmCons = "";
        String verCons = "";
        String cmpairOP = "";

        if (S.contains("<=")) {
            S = S.replace("<", "");
            versionConsName = S.split("=");
            nmCons = versionConsName[0];
            verCons = versionConsName[1];
            cmpairOP = "<=";
        } else if (S.contains("<")) {
            versionConsName = S.split("<");
            nmCons = versionConsName[0];
            verCons = versionConsName[1];
            cmpairOP = "<";
        } else if (S.contains(">=")) {
            S = S.replace(">", "");
            versionConsName = S.split("=");
            nmCons = versionConsName[0];
            verCons = versionConsName[1];
            cmpairOP = ">=";
        } else if (S.contains(">")) {
            versionConsName = S.split(">");
            nmCons = versionConsName[0];
            verCons = versionConsName[1];
            cmpairOP = ">";
        } else if (S.contains("=")) {
            versionConsName = S.split("=");
            nmCons = versionConsName[0];
            verCons = versionConsName[1];
            cmpairOP = "=";
        } else nmCons = S;

        String[] solutionArray = { nmCons, verCons, cmpairOP };
        return solutionArray;
    }

    /**
     * Returns the cost of a given solution
     *
     * @param repo
     * @return
     */
    private static int price(List<Package> repo) {
        int solutionArray = 0;

        for (String T : commandsArray) {

            if (T.contains("-")) solutionArray+= 1000000;
            else {
                for (Package pack : repo) {
                    String versionName;
                    versionName = pack.getName() + "=" + pack.getVersion();

                    String p;
                    p = T.substring(1);

                    if (p.equals(versionName)) solutionArray = solutionArray + pack.getSize();
                }
            }
        }

        return solutionArray;
    }

    /**
     * Outputs the cheapest solution to the console in a JSON format.
     */
    private static void getANS() {
        List<String> solutionArray = new ArrayList<>();
        int resultCost = 0;
        for (Map.Entry<String, Integer> entry : solutionHash.entrySet()) {
            if (solutionArray.size() == 0 || entry.getValue() < resultCost) {
                solutionArray = Arrays.asList(entry.getKey().split(","));
                resultCost = entry.getValue();
            }
        }
        System.out.println(JSON.toJSON(solutionArray));
    }

    /**
     * Reads a selected file
     *
     * @param filename
     * @return A string containing read in file
     * @throws IOException
     */
    static String readFile(String filename) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(filename));
        StringBuilder sb = new StringBuilder();
        br.lines().forEach(line -> sb.append(line));
        return sb.toString();
    }
}