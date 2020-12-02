package fr.uga.pddl4j.problem;

import fr.uga.pddl4j.encoding.*;
import fr.uga.pddl4j.parser.*;

import java.util.*;

/**
 * Created by pellier on 01/12/2020.
 */
public class ADLProblem extends PostInstantiatedProblem {

    /**
     * The list of instantiated actions encoded into bit sets.
     */
    private List<Action> actions;

    /**
     * The list of instantiated methods encoded into bit sets.
     */
    private List<Method> methods;

    private Map<IntExpression, Integer> mapOfFluentIndex;

    private Map<IntExpression, Integer> mapOfNumericFluentIndex;
    private Map<IntExpression, Integer> mapOfTasksIndex;

    private Goal goal;

    /**
     * The initial task network.
     */
    private TaskNetwork initialTaskNetwork;

    /**
     * The table containing for each relevant task its set of resolvers, i.e., action or methods
     */
    private List<List<Integer>> tableOfRelevantOperators;


    private InitialState init;

    public ADLProblem(final PDDLDomain domain, final PDDLProblem problem) {
        super(domain, problem);
    }

    public void instantiate(long timout) {
        this.preInstantiate();
        this.instantiate();
        this.postInstantiate();
        this.completeInstantiation();
    }

    /**
     * Returns <code>true</code> if this problem is solvable. In the case of STRIPS planning, the method returns
     * <code>false</code> if the goal is simplified to <code>false</code> during the encoding process, otherwise the
     * method returns <code>true</code>. In the case of HTN planning, the method returns <code>false</code> if at least
     * one of the task of the initial task network is not reachable after the encoding process, i.e., as a task is set
     * to null in the tasks list of the initial task network, otherwise the method returns <code>true</code>.
     * <p>
     * Warning, it is not because the method returns <code>true</code> that the problem is solvable. It just means that
     * the encoding process can not exclude the fact that the problem is solvable.
     * </p>
     *
     * @return <code>true</code> if this problem is solvable; <code>false</code>.
     */
    public final boolean isSolvable() {
        boolean isSovable = true;
        if (this.getRequirements().contains(PDDLRequireKey.HIERARCHY)) {
            Iterator<Integer> i = this.initialTaskNetwork.getTasks().iterator();
            while (i.hasNext() && isSovable) {
                isSovable = i.next() != null;
            }
        } else {
            isSovable = this.goal != null;
        }
        return isSovable;
    }

    public List<Action> getActions() {
        return actions;
    }

    public List<Method> getMethods() {
        return methods;
    }

    protected Map<IntExpression, Integer> getMapOfFluentIndex() {
        return mapOfFluentIndex;
    }

    protected Map<IntExpression, Integer> getMapOfNumericFluentIndex() {
        return mapOfNumericFluentIndex;
    }

    public Map<IntExpression, Integer> getMapOfTasksIndex() {
        return mapOfTasksIndex;
    }

    public List<List<Integer>> getRelevantOperators() {
        return tableOfRelevantOperators;
    }

    public Goal getGoal() {
        return goal;
    }

    public TaskNetwork getInitialTaskNetwork() {
        return initialTaskNetwork;
    }

    public InitialState getInitialState() {
        return init;
    }

    protected void completeInstantiation() {
        // Creates the final list of actions and methods that will be used in the problem
        this.actions = new ArrayList<>(this.getIntActions().size());
        if (this.getRequirements().contains(PDDLRequireKey.HIERARCHY)) {
            this.methods = new ArrayList<>(this.getIntMethods().size());
        }

        this.initOfMapFluentIndex();
        if (this.getRequirements().contains(PDDLRequireKey.NUMERIC_FLUENTS)) {
            this.initMapOfNumericFluentIndex();
        }

        if (this.getRequirements().contains(PDDLRequireKey.HIERARCHY)) {
            this.initRelevantOperators();
            this.initMapOfTaskIndex();
        }

        this.encodeGoal();

        if (this.getRequirements().contains(PDDLRequireKey.HIERARCHY)) {
            this.encodeInitialTaskNetwork();
            this.encodeMethods();
        }

        this.encodeInit();
        if (this.getRequirements().contains(PDDLRequireKey.NUMERIC_FLUENTS)) {
            this.encodeInitNumericFluent();
        }
        if (this.getRequirements().contains(PDDLRequireKey.DURATIVE_ACTIONS)) {
            NumericVariable duration = new NumericVariable(NumericVariable.DURATION, 0.0);
            this.init.addNumericFluent(duration);
        }
        this.encodeActions(this.getIntActions());

    }

    protected void initOfMapFluentIndex() {
        // Create a map of the relevant fluents with their index to speedup the bit set encoding of the actions
        this.mapOfFluentIndex = new LinkedHashMap<>(this.getRelevantFluents().size());
        int index = 0;
        for (IntExpression fluent : this.getRelevantFluents()) {
            this.mapOfFluentIndex.put(fluent, index);
            index++;
        }
    }

    protected void initMapOfTaskIndex() {
        // Create a map of the relevant tasks with their index to speedup the bit set encoding of the methods
        this.mapOfTasksIndex = new LinkedHashMap<>(this.getRelevantTasks().size());
        int index = 0;
        for (IntExpression task : this.getRelevantTasks()) {
            this.mapOfTasksIndex.put(task, index);
            index++;

        }
    }

    /**
     * Create a map of the relevant numeric fluents with their index to speedup the bit set encoding of the actions
     */
    protected void initMapOfNumericFluentIndex() {
        this.mapOfNumericFluentIndex = new LinkedHashMap<>(this.getTableOfRelevantNumericFluents().size());
        int index = 0;
        for (IntExpression fluent : this.getTableOfRelevantNumericFluents()) {
            this.mapOfNumericFluentIndex.put(fluent, index);
        }
    }

    protected void initRelevantOperators() {
        this.tableOfRelevantOperators = new ArrayList<>();
        for (Integer a : this.getRelevantActions()) {
            List<Integer> l = new ArrayList<>(1);
            l.add(a);
            this.tableOfRelevantOperators.add(l);
        }
        this.tableOfRelevantOperators.addAll(this.getRelevantMethods());
    }

    /**
     * Encode a list of specified actions into <code>BitSet</code> representation. The specified
     * map is used to speed-up the search by mapping the an expression to this index.
     *
     */
    protected void encodeActions(final List<IntAction> actions)
        throws UnexpectedExpressionException {

        final List<Action> encodedActions = new ArrayList<>(actions.size());
        final List<Action> addedActions = new ArrayList<>();
        int actionIndex = 0;
        for (IntAction intAction : actions) {
            List<IntAction> normalized = this.normalizeAction(intAction);
            encodedActions.add(this.encodeAction(normalized.get(0)));
            for (int i  = 1; i < normalized.size(); i++) {
                if (this.getRelevantOperators() != null) {
                    this.getRelevantOperators().get(actionIndex).add(actions.size() + addedActions.size());
                }
                addedActions.add(this.encodeAction(normalized.get(i)));
            }
            actionIndex++;
        }
        encodedActions.addAll(addedActions);
        this.actions.addAll(encodedActions);
    }

    /**
     * Encode an specified <code>IntExpression</code> in its <code>BitExp</code> representation.The
     * specified map is used to speed-up the search by mapping the an expression to this index.
     *
     * @return the expression in bit set representation.
     */
    private Effect encodeEffect(final IntExpression exp)
        throws UnexpectedExpressionException {
        final Effect effect = new Effect();
        switch (exp.getConnective()) {
            case ATOM:
                Integer index = this.mapOfFluentIndex.get(exp);
                if (index != null) {
                    effect.getPositiveFluents().set(index);
                }
                break;
            case NOT:
                index = this.mapOfFluentIndex.get(exp.getChildren().get(0));
                if (index != null) {
                    effect.getNegativeFluents().set(index);
                }
                break;
            case AND:
                final List<IntExpression> children = exp.getChildren();
                for (IntExpression ei : children) {
                    switch (ei.getConnective()) {
                        case ATOM:
                            index = this.mapOfFluentIndex.get(ei);
                            if (index != null) {
                                effect.getPositiveFluents().set(index);
                            }
                            break;
                        case NOT:
                            index = this.mapOfFluentIndex.get(ei.getChildren().get(0));
                            if (index != null) {
                                effect.getNegativeFluents().set(index);
                            }
                            break;
                        case TRUE:
                            // do nothing
                            break;
                        case ASSIGN:
                        case INCREASE:
                        case DECREASE:
                        case SCALE_UP:
                        case SCALE_DOWN:
                            NumericAssignment assignment = this.encodeNumericAssignment(ei);
                            effect.addNumericAssignment(assignment);
                            break;
                        default:
                            throw new UnexpectedExpressionException(ei.getConnective().toString());
                    }
                }
                break;
            case ASSIGN:
            case INCREASE:
            case DECREASE:
            case SCALE_UP:
            case SCALE_DOWN:
                NumericAssignment assignment = this.encodeNumericAssignment(exp);
                effect.addNumericAssignment(assignment);
                break;
            case TRUE:
                // Do nothing
                break;
            default:
                throw new UnexpectedExpressionException(exp.getConnective().toString());
        }
        return effect;
    }

    /**
     * Normalize an action, i.e, put in disjunctive normal form (DNF) for preconditions and put
     * in conjunctive normal form (CNF) for effects. If an action has disjunctive preconditions, a
     * new operator is created such all actions after normalization have only conjunctive
     * precondition.
     *
     * @param action the action to normalize.
     */
    private List<IntAction> normalizeAction(final IntAction action) {
        final List<IntAction> normalisedActions = new ArrayList<>();
        this.toCNF(action.getEffects());
        this.simplify(action.getEffects());
        final IntExpression precond = action.getPreconditions();
        this.toDNF(precond);
        if (precond.getChildren().size() > 0) {
            for (final IntExpression ei : precond.getChildren()) {
                final String name = action.getName();
                final int arity = action.arity();
                final IntAction newAction = new IntAction(name, arity);
                newAction.setCost(action.getCost());
                for (int i = 0; i < arity; i++) {
                    newAction.setTypeOfParameter(i, action.getTypeOfParameters(i));
                }
                for (int i = 0; i < arity; i++) {
                    newAction.setValueOfParameter(i, action.getValueOfParameter(i));
                }
                if (action.isDurative()) {
                    newAction.setDuration(new IntExpression(action.getDuration()));
                }
                newAction.setPreconditions(ei);

                newAction.setEffects(new IntExpression(action.getEffects()));
                normalisedActions.add(newAction);
            }
        } else {
            normalisedActions.add(action);
        }
        return normalisedActions;
    }

    /**
     * Encodes a numeric assignment.
     */
    protected NumericAssignment encodeNumericAssignment(final IntExpression exp) {

        final NumericVariable fluent = new NumericVariable(this.mapOfNumericFluentIndex.get(exp.getChildren().get(0)));
        final ArithmeticExpression arithmeticExpression = this.encodeArithmeticExpression(exp.getChildren().get(1));
        NumericAssignment assignment = null;
        switch (exp.getConnective()) {
            case ASSIGN:
                assignment = new NumericAssignment(NumericAssignment.Operator.ASSIGN, fluent, arithmeticExpression);
                break;
            case INCREASE:
                assignment = new NumericAssignment(NumericAssignment.Operator.INCREASE, fluent, arithmeticExpression);
                break;
            case DECREASE:
                assignment = new NumericAssignment(NumericAssignment.Operator.DECREASE, fluent, arithmeticExpression);
                break;
            case SCALE_UP:
                assignment = new NumericAssignment(NumericAssignment.Operator.SCALE_UP, fluent, arithmeticExpression);
                break;
            case SCALE_DOWN:
                assignment = new NumericAssignment(NumericAssignment.Operator.SCALE_DOWN, fluent, arithmeticExpression);
                break;
            default:
                throw new UnexpectedExpressionException(exp.getConnective().toString());
        }
        return assignment;
    }

    /**
     * Encodes a specified action.
     *
     * @param action the action to be encoded. The precondition of the action must be a simple conjunction of atomic
     *               formulas
     * @param fluents the map that associates to a specified expression its index.
     * @return the action encoded.
     */
    private Action encodeAction(final IntAction action) {
        final int arity = action.arity();
        final Action encoded = new Action(action.getName(), arity);
        encoded.setCost(new NumericVariable(-1, action.getCost()));

        // Initialize the parameters of the action
        for (int i = 0; i < arity; i++) {
            encoded.setValueOfParameter(i, action.getValueOfParameter(i));
            encoded.setTypeOfParameter(i, action.getTypeOfParameters(i));
        }

        if (action.isDurative()) {
            List<NumericConstraint> duration = this.encodeNumericConstraints(action.getDuration());
            encoded.setDurationConstraints(duration);
        }

        // Initialize the preconditions of the action
        encoded.setPrecondition(this.encodeCondition(action.getPreconditions()));

        // Initialize the effects of the action
        final List<IntExpression> effects = action.getEffects().getChildren();

        final ConditionalEffect unCondEffects = new ConditionalEffect();
        boolean hasUnConditionalEffects = false;
        for (IntExpression ei : effects) {
            final PDDLConnective connective = ei.getConnective();
            final List<IntExpression> children = ei.getChildren();
            switch (connective) {
                case WHEN:
                    // NumericAssignement not encoded in conditional effect.
                    final ConditionalEffect condBitExp = new ConditionalEffect();
                    condBitExp.setCondition(this.encodeCondition(children.get(0)));
                    condBitExp.setEffect(this.encodeEffect(children.get(1)));
                    encoded.getConditionalEffects().add(condBitExp);
                    break;
                case ATOM:
                    Integer index = this.mapOfFluentIndex.get(ei);
                    if (index != null) {
                        unCondEffects.getEffect().getPositiveFluents().set(index);
                        hasUnConditionalEffects = true;
                    }
                    break;
                case NOT:
                    index = this.mapOfFluentIndex.get(children.get(0));
                    if (index != null) {
                        unCondEffects.getEffect().getNegativeFluents().set(index);
                        hasUnConditionalEffects = true;
                    }
                    break;
                case TRUE:
                    // do nothing
                    break;
                case ASSIGN:
                case SCALE_DOWN:
                case SCALE_UP:
                case INCREASE:
                case DECREASE:
                    NumericAssignment assignment = this.encodeNumericAssignment(ei);
                    unCondEffects.getEffect().addNumericAssignment(assignment);
                    break;
                default:
                    throw new UnexpectedExpressionException(ei.getConnective().toString());
            }
        }
        if (hasUnConditionalEffects) {
            encoded.getConditionalEffects().add(unCondEffects);
        }
        return encoded;
    }

    /**
     * Encode a specified goal in a disjunction of <code>BitExp</code>. The specified
     * map is used to speed-up the search by mapping the an expression to this index.
     *
     * @return a list of <code>BitExp</code> that represents the goal as a disjunction of
     * <code>BitExp</code>.
     */
    protected void encodeGoal() {

        if (this.getIntGoal().getConnective().equals(PDDLConnective.FALSE)) {
            this.goal = null;
        } else {
            this.toDNF(this.getIntGoal());
            List<Goal> goals = new ArrayList<>(this.getIntGoal().getChildren().size());
            for (IntExpression exp : this.getIntGoal().getChildren()) {
                if (exp.getConnective().equals(PDDLConnective.ATOM)) {
                    IntExpression and = new IntExpression(PDDLConnective.AND);
                    and.getChildren().add(exp);
                    goals.add(new Goal(this.encodeCondition(and)));
                } else {
                    goals.add(new Goal(this.encodeCondition(exp)));
                }
            }
            if (goals.size() > 1) {
                // Create a new dummy fact to encode the goal
                final int dummyPredicateIndex = this.getPredicateSymbols().size();
                this.getPredicateSymbols().add(Constants.DUMMY_GOAL);
                this.getPredicateSignatures().add(new ArrayList<>());
                IntExpression dummyGoal = new IntExpression(PDDLConnective.ATOM);
                dummyGoal.setPredicate(dummyPredicateIndex);
                dummyGoal.setArguments(new int[0]);
                final int dummyGoalIndex = this.getRelevantFluents().size();
                this.getRelevantFluents().add(dummyGoal);
                this.mapOfNumericFluentIndex.put(dummyGoal, dummyGoalIndex);
                Effect effect = new Effect();
                effect.getPositiveFluents().set(dummyGoalIndex);
                this.goal = new Goal();
                effect.getPositiveFluents().set(dummyGoalIndex);
                final ConditionalEffect condEffect = new ConditionalEffect(effect);
                // for each disjunction create a dummy action
                for (Condition dis : goals) {
                    final Action op = new Action(Constants.DUMMY_OPERATOR, 0);
                    op.setDummy(true);
                    op.setPrecondition(dis);
                    op.getConditionalEffects().add(condEffect);
                    this.getActions().add(op);
                }
            } else {
                this.goal = goals.get(0);
            }
        }
    }

    /**
     * Encode an specified <code>IntExpression</code> in its <code>BitExp</code> representation.The
     * specified map is used to speed-up the search by mapping the an expression to this index.
     *
     * @param exp the <code>IntExpression</code>.
     */
    private Condition encodeCondition(final IntExpression exp) throws UnexpectedExpressionException {
        final Condition condition = new Condition();
        switch (exp.getConnective()) {
            case ATOM:
                condition.getPositiveFluents().set(this.getMapOfFluentIndex().get(exp));
                break;
            case NOT:
                condition.getNegativeFluents().set(this.getMapOfFluentIndex().get(exp.getChildren().get(0)));
                break;
            case AND:
                for (IntExpression e : exp.getChildren()) {
                    Condition ce = this.encodeCondition(e);
                    condition.getPositiveFluents().or(ce.getPositiveFluents());
                    condition.getNegativeFluents().or(ce.getNegativeFluents());
                    condition.getNumericConstraints().addAll(ce.getNumericConstraints());
                }
                break;
            case LESS:
            case LESS_OR_EQUAL:
            case GREATER:
            case GREATER_OR_EQUAL:
            case EQUAL:
                condition.getNumericConstraints().add(this.encodeNumericConstraint(exp));
                break;
            case TRUE:
                // do nothing
                break;
            default:
                throw new UnexpectedExpressionException(exp.getConnective().toString());
        }
        return condition;
    }

    /**
     * Encode an specified <code>IntExpression</code> in its <code>BitExp</code> representation.The
     * specified map is used to speed-up the search by mapping the an expression to this index.
     *
     * @param exp the <code>IntExpression</code>.
     * @return the expression in bit set representation.
     */
    private List<NumericConstraint> encodeNumericConstraints(final IntExpression exp)
        throws UnexpectedExpressionException {

        final List<NumericConstraint> constraints = new ArrayList<>();
        switch (exp.getConnective()) {
            case AND:
                for (IntExpression e : exp.getChildren()) {
                    constraints.addAll(this.encodeNumericConstraints(e));
                }
                break;
            case LESS:
            case LESS_OR_EQUAL:
            case GREATER:
            case GREATER_OR_EQUAL:
            case EQUAL:
                constraints.add(this.encodeNumericConstraint(exp));
                break;
            case TRUE:
                // do nothing
                break;
            default:
                throw new UnexpectedExpressionException(exp.getConnective().toString());
        }
        return constraints;
    }

    /**
     * Encodes a numeric constraint.
     */
    private NumericConstraint encodeNumericConstraint(final IntExpression exp) {

        ArithmeticExpression left = this.encodeArithmeticExpression(exp.getChildren().get(0));
        ArithmeticExpression right = this.encodeArithmeticExpression(exp.getChildren().get(1));
        NumericConstraint constraint;
        switch (exp.getConnective()) {
            case EQUAL:
                constraint = new NumericConstraint(NumericConstraint.Comparator.EQUAL, left, right);
                break;
            case LESS:
                constraint = new NumericConstraint(NumericConstraint.Comparator.LESS, left, right);
                break;
            case LESS_OR_EQUAL:
                constraint = new NumericConstraint(NumericConstraint.Comparator.LESS_OR_EQUAL, left, right);
                break;
            case GREATER:
                constraint = new NumericConstraint(NumericConstraint.Comparator.GREATER, left, right);
                break;
            case GREATER_OR_EQUAL:
                constraint = new NumericConstraint(NumericConstraint.Comparator.GREATER_OR_EQUAL, left, right);
                break;
            default:
                throw new UnexpectedExpressionException(exp.getConnective().toString());
        }
        return constraint;
    }
    /**
     * Encodes an arithmetic expression.
     *
     * @param exp the expression to encode.
     */
    private ArithmeticExpression encodeArithmeticExpression(final IntExpression exp) {

        ArithmeticExpression arithmeticExpression;
        ArithmeticExpression left;
        ArithmeticExpression right;
        switch (exp.getConnective()) {
            case PLUS:
                left = this.encodeArithmeticExpression(exp.getChildren().get(0));
                right = this.encodeArithmeticExpression(exp.getChildren().get(1));
                arithmeticExpression = new ArithmeticExpression(ArithmeticOperator.PLUS, left, right);
                break;
            case MINUS:
                left = this.encodeArithmeticExpression(exp.getChildren().get(0));
                right = this.encodeArithmeticExpression(exp.getChildren().get(1));
                arithmeticExpression = new ArithmeticExpression(ArithmeticOperator.MINUS, left, right);
                break;
            case UMINUS:
                left = this.encodeArithmeticExpression(exp.getChildren().get(0));
                arithmeticExpression = new ArithmeticExpression(ArithmeticOperator.UMINUS, left, null);
                break;
            case DIV:
                left = this.encodeArithmeticExpression(exp.getChildren().get(0));
                right = this.encodeArithmeticExpression(exp.getChildren().get(1));
                arithmeticExpression = new ArithmeticExpression(ArithmeticOperator.DIV, left, right);
                break;
            case MUL:
                left = this.encodeArithmeticExpression(exp.getChildren().get(0));
                right = this.encodeArithmeticExpression(exp.getChildren().get(1));
                arithmeticExpression = new ArithmeticExpression(ArithmeticOperator.MUL, left, right);
                break;
            case NUMBER:
                arithmeticExpression = new ArithmeticExpression(exp.getValue());
                break;
            case F_EXP:
                arithmeticExpression = this.encodeArithmeticExpression(exp.getChildren().get(0));
                break;
            case FN_HEAD:
                arithmeticExpression = new NumericVariable(this.getMapOfNumericFluentIndex().get(exp));
                break;
            case TIME_VAR:
                arithmeticExpression = new NumericVariable(NumericVariable.DURATION);
                break;
            default:
                throw new UnexpectedExpressionException(exp.getConnective().toString());
        }
        return arithmeticExpression;
    }

    /**
     * Convert an expression in conjunctive normal form (CNF).
     *
     * @param exp the expression to transform in CNF.
     */
    private void toCNF(final IntExpression exp) throws UnexpectedExpressionException {
        switch (exp.getConnective()) {
            case WHEN:
                final IntExpression antecedent = exp.getChildren().get(0);
                final IntExpression consequence = exp.getChildren().get(1);
                this.toDNF(antecedent);
                exp.setConnective(PDDLConnective.AND);
                exp.getChildren().clear();
                for (IntExpression ei : antecedent.getChildren()) {
                    final IntExpression newWhen = new IntExpression(PDDLConnective.WHEN);
                    newWhen.getChildren().add(ei);
                    newWhen.getChildren().add(new IntExpression(consequence));
                    exp.getChildren().add(newWhen);
                }
                break;
            case AND:
                final List<IntExpression> children = exp.getChildren();
                int i = 0;
                while (i < children.size()) {
                    final IntExpression ei = children.get(i);
                    this.toCNF(ei);
                    exp.getChildren().remove(i);
                    for (IntExpression ej : ei.getChildren()) {
                        exp.getChildren().add(i, ej);
                        i++;
                    }
                }
                break;
            case ATOM:
            case AT_END:
            case AT_START:
            case OVER_ALL:
            case INCREASE:
            case DECREASE:
            case ASSIGN:
            case SCALE_UP:
            case SCALE_DOWN:
            case NOT:
            case TRUE:
                IntExpression copy = new IntExpression(exp);
                exp.setConnective(PDDLConnective.AND);
                exp.getChildren().clear();
                exp.getChildren().add(copy);
                break;
            default:
                throw new UnexpectedExpressionException(exp.getConnective().toString());
        }
    }
    /**
     * Convert an expression in disjunctive normal form (DNF).
     *
     * @param exp the expression to transform in DNF.
     */
    private void toDNF(final IntExpression exp) throws UnexpectedExpressionException {
        switch (exp.getConnective()) {
            case OR:
                List<IntExpression> children = exp.getChildren();
                int index = 0;
                while (index < children.size()) {
                    final IntExpression ei = children.get(index);
                    this.toDNF(ei);
                    if (ei.getConnective().equals(PDDLConnective.OR)) {
                        children.remove(index);
                        for (IntExpression ej : ei.getChildren()) {
                            children.add(index, ej);
                            index++;
                        }
                    }
                }
                break;
            case AND:
                children = exp.getChildren();
                for (IntExpression child : children) {
                    this.toDNF(child);
                }
                IntExpression dnf = exp.getChildren().get(0);
                for (int i = 1; i < exp.getChildren().size(); i++) {
                    final IntExpression orExp = exp.getChildren().get(i);
                    final IntExpression newOr = new IntExpression(PDDLConnective.OR);
                    for (IntExpression newAnd : dnf.getChildren()) {
                        for (IntExpression ek : orExp.getChildren()) {
                            ek.getChildren().stream().filter(el -> !newAnd.getChildren().contains(el)).forEach(el -> {
                                if (el.getConnective().equals(PDDLConnective.OR)
                                    || el.getConnective().equals(PDDLConnective.AND)
                                    && el.getChildren().size() == 1) {
                                    newAnd.getChildren().add(el.getChildren().get(0));
                                } else {
                                    newAnd.getChildren().add(el);
                                }
                            });
                            boolean add = true;
                            for (IntExpression el : newAnd.getChildren()) {
                                if (el.getConnective().equals(PDDLConnective.FALSE)) {
                                    add = false;
                                    break;
                                }
                            }
                            if (add) {
                                if (newAnd.getChildren().size() == 1) {
                                    newOr.getChildren().add(newAnd.getChildren().get(0));
                                } else {
                                    newOr.getChildren().add(newAnd);
                                }
                            }
                        }
                    }
                    dnf = newOr;
                }
                exp.affect(dnf);
                break;
            case ATOM:
            case NOT:
            case LESS:
            case LESS_OR_EQUAL:
            case GREATER:
            case GREATER_OR_EQUAL:
            case EQUAL:
            case EQUAL_ATOM:
            case INCREASE:
            case DECREASE:
            case SCALE_UP:
            case SCALE_DOWN:
            case AT_END:
            case AT_START:
            case OVER_ALL:
            case FALSE:
            case TRUE:
                IntExpression and = new IntExpression(PDDLConnective.AND);
                and.getChildren().add(new IntExpression(exp));
                exp.setConnective(PDDLConnective.OR);
                exp.getChildren().clear();
                exp.getChildren().add(and);
                break;
            default:
                throw new UnexpectedExpressionException(exp.getConnective().toString());
        }
    }

    protected void encodeInitialTaskNetwork() {
        this.initialTaskNetwork = this.encodeTaskNetwork(this.getIntInitialTaskNetwork());
    }

    /**
     * Encode a specified task network.
     * map is used to speed-up the search by mapping the an expression to this index.
     *
     * @param taskNetwork the tasknetwork to encode.
     * @return a list of <code>BitExp</code> that represents the goal as a disjunction of
     * <code>BitExp</code>.
     */
    protected TaskNetwork encodeTaskNetwork(IntTaskNetwork taskNetwork) {
        // We encode first the tasks
        final List<Integer> tasks = new ArrayList<Integer>();
        this.encodeTasks(taskNetwork.getTasks(), tasks);
        // We encode then the ordering constraints
        final OrderingConstraintSet constraints = new OrderingConstraintSet(tasks.size());
        for (IntExpression c : taskNetwork.getOrderingConstraints().getChildren()) {
            constraints.set(c.getChildren().get(0).getTaskID(), c.getChildren().get(1).getTaskID());
        }
        final TaskNetwork tn = new TaskNetwork(tasks, constraints);
        tn.transitiveClosure();
        return tn;
    }

    /**
     * Encode the list of tasks expressed as an IntExpression into a list of integer.
     *
     * @param exp   the list of tasks expressed as an IntExpression.
     * @param tasks the list of task encoded as integer.
     */
    private void encodeTasks(IntExpression exp, List<Integer> tasks) {
        switch (exp.getConnective()) {
            case TASK:
                tasks.add(this.mapOfTasksIndex.get(exp));
                break;
            case AND:
            case OR:
                for (IntExpression e : exp.getChildren()) {
                    this.encodeTasks(e, tasks);
                }
                break;
            default:
                // Do nothing
        }
    }

    /**
     * Encode a list of specified methods into the final compact representation. The specified
     * maps are used to speed-up the search by mapping the an expression to this index.
     *
     * @return the list of methods encoded into final compact representation.
     */
    protected void encodeMethods() throws UnexpectedExpressionException {
        this.methods = new ArrayList<>(methods.size());
        final List<Method> addedMethods = new ArrayList<>();
        int methodIndex = this.getRelevantActions().size();
        for (IntMethod intMethod : this.getIntMethods()) {
            List<IntMethod> normalized = this.normalizeMethod(intMethod);
            this.methods.add(this.encodeMethod(normalized.get(0), this.mapOfFluentIndex, this.mapOfTasksIndex));
            for (int i  = 1; i < normalized.size(); i++) {
                if (this.getRelevantOperators() != null) {
                    this.getRelevantOperators().get(methodIndex).add(methods.size() + addedMethods.size());
                }
                this.methods.add(this.encodeMethod(normalized.get(i), this.mapOfFluentIndex, this.mapOfTasksIndex));
            }
            methodIndex++;
        }
        this.methods.addAll(addedMethods);
    }

    /**
     * Encode a list of specified methods into the final compact representation. The specified
     * maps are used to speed-up the search by mapping the an expression to this index.
     *
     * @param method the list of methods to encode.
     * @param factMap the map that associates at a specified fact its index in the table of relevant fluents.
     * @param taskMap the map that associates at a specified task its index in the table of relevant tasks.
     * @return the list of methods encoded into final compact representation.
     */
    protected Method encodeMethod(final IntMethod method, final Map<IntExpression, Integer> factMap,
                               final Map<IntExpression, Integer> taskMap) throws UnexpectedExpressionException {

        final int arity = method.arity();
        final Method encoded = new Method(method.getName(), arity);
        // Initialize the parameters of the method
        for (int i = 0; i < arity; i++) {
            encoded.setValueOfParameter(i, method.getValueOfParameter(i));
            encoded.setTypeOfParameter(i, method.getTypeOfParameters(i));
        }
        // Encode the task carried out by the method
        encoded.setTask(taskMap.get(method.getTask()));
        // Encode the preconditions of the method
        encoded.setPrecondition(this.encodeCondition(method.getPreconditions()));
        // Encode the task network of the method
        encoded.setTaskNetwork(this.encodeTaskNetwork(method.getTaskNetwork()));
        return encoded;
    }

    /**
     * Normalize the methods, i.e, put in disjunctive normal form (DNF) the preconditions. If a method has
     * disjunctive preconditions, a new method is created such all methods after normalization have only conjunctive
     * precondition.
     *
     * @param method the list of methods to normalizeActions.
     */
    private List<IntMethod> normalizeMethod(final IntMethod method) throws UnexpectedExpressionException {
        final List<IntMethod> normalisedMethods = new ArrayList<>();
        final IntExpression precond = method.getPreconditions();
        this.toDNF(precond);
        if (precond.getChildren().size() > 0) {
            for (final IntExpression ei : precond.getChildren()) {
                final String name = method.getName();
                final int arity = method.arity();
                final IntMethod newMethod = new IntMethod(name, arity);
                for (int i = 0; i < arity; i++) {
                    newMethod.setTypeOfParameter(i, method.getTypeOfParameters(i));
                }
                for (int i = 0; i < arity; i++) {
                    newMethod.setValueOfParameter(i, method.getValueOfParameter(i));
                }
                newMethod.setPreconditions(ei);
                newMethod.setTask(new IntExpression(method.getTask()));
                newMethod.setTaskNetwork(new IntTaskNetwork(method.getTaskNetwork()));
                normalisedMethods.add(newMethod);
            }
        } else {
            normalisedMethods.add(method);
        }
        return normalisedMethods;
    }

    /**
     * Encode a specified initial state in it <code>BitExp</code> representation. The specified
     * map is used to speed-up the search by mapping the an expression to this index.
     *
     * @return the <code>BitExp</code> that represents the initial encoded.
     */
    private void encodeInit() {
        this.init = new InitialState();
        for (final IntExpression fact : this.getIntInitPredicates()) {
            switch (fact.getConnective()) {
                case ATOM:
                    Integer i = this.mapOfFluentIndex.get(fact);
                    if (i != null) {
                        this.init.getPositiveFluents().set(i);
                    }
                    break;
                case NOT:
                    i = this.mapOfFluentIndex.get(fact.getChildren().get(0));
                    if (i != null) {
                        this.init.getNegativeFluents().set(i);
                    }
                    break;
            }
        }
    }

    /**
     * Encode the numeric fluent of the initial state.
     */
    private void encodeInitNumericFluent() {
        for (Map.Entry<IntExpression, Integer> e : this.mapOfNumericFluentIndex.entrySet()) {
            int index = e.getValue();
            double value = this.getIntInitFunctionCost().get(e.getKey());
            NumericVariable fluent = new NumericVariable(index, value);
            this.init.addNumericFluent(fluent);
        }
    }
}