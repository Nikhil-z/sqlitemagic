package com.siimkinks.sqlitemagic;

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Size;

import java.util.ArrayList;
import java.util.LinkedList;

import static com.siimkinks.sqlitemagic.StringArraySet.BASE_SIZE;
import static com.siimkinks.sqlitemagic.Table.ANONYMOUS_TABLE;
import static com.siimkinks.sqlitemagic.Utils.DOUBLE_PARSER;
import static com.siimkinks.sqlitemagic.Utils.LONG_PARSER;
import static com.siimkinks.sqlitemagic.Utils.STRING_PARSER;
import static com.siimkinks.sqlitemagic.Utils.parserForNumberType;

/**
 * Builder for SQL SELECT statement.
 *
 * @param <S> Selection type -- either {@link Select1} or {@link SelectN}
 */
public final class Select<S> extends SelectSqlNode<S> {
	/**
	 * Interface that represents single column selection
	 */
	public interface Select1 {
	}

	/**
	 * Interface that represents n column selection
	 */
	public interface SelectN {
	}

	private static final Column<?, ?, ?, ?>[] ALL = new Column<?, ?, ?, ?>[0];

	@NonNull
	private final String stmt;

	Select(@NonNull String stmt) {
		super(null);
		this.stmt = stmt;
	}

	@Override
	void appendSql(@NonNull StringBuilder sb) {
		sb.append(stmt);
	}

	@Override
	void appendSql(@NonNull StringBuilder sb, @NonNull SimpleArrayMap<String, LinkedList<String>> systemRenamedTables) {
		sb.append(stmt);
	}

	/**
	 * Select all columns.
	 * <p>
	 * Equivalent to statement SELECT * [...]
	 *
	 * @return SQL SELECT statement builder
	 */
	@CheckResult
	public static Columns all() {
		return new Columns(new Select<SelectN>("SELECT"), ALL);
	}

	/**
	 * Select single column.
	 *
	 * @param column Column to select. This param must be one of annotation processor
	 *               generated column objects that corresponds to column in a database
	 *               table
	 * @param <R>    Java type that column represents
	 * @return SQL SELECT statement builder
	 */
	@CheckResult
	public static <R> SingleColumn<R> column(@NonNull Column<?, R, ?, ?> column) {
		return new SingleColumn<>(new Select<Select1>("SELECT"), column);
	}

	/**
	 * Select multiple columns.
	 *
	 * @param columns Columns to select. These params must be one of annotation processor
	 *                generated column objects that corresponds to column in a database
	 *                table
	 * @return SQL SELECT statement builder
	 */
	@CheckResult
	public static Columns columns(@NonNull @Size(min = 1) Column<?, ?, ?, ?>... columns) {
		return new Columns(new Select<SelectN>("SELECT"), columns);
	}

	/**
	 * Select distinct columns.
	 * <p>
	 * Creates "SELECT DISTINCT * ..." query builder where duplicate rows
	 * are removed from the set of result rows. For the purposes of detecting duplicate
	 * rows, two NULL values are considered to be equal.
	 *
	 * @return SQL SELECT statement builder
	 */
	@CheckResult
	public static Columns distinct() {
		return new Columns(new Select<SelectN>("SELECT DISTINCT"), ALL);
	}

	/**
	 * Select single distinct column.
	 * <p>
	 * Creates "SELECT DISTINCT {@code column} ..." query builder where duplicate rows
	 * are removed from the set of result rows. For the purposes of detecting duplicate
	 * rows, two NULL values are considered to be equal.
	 *
	 * @param column Column to select. This param must be one of annotation processor
	 *               generated column objects that corresponds to column in a database
	 *               table
	 * @param <R>    Java type that column represents
	 * @return SQL SELECT statement builder
	 */
	@CheckResult
	public static <R> SingleColumn<R> distinct(@NonNull Column<?, R, ?, ?> column) {
		return new SingleColumn<>(new Select<Select1>("SELECT DISTINCT"), column);
	}

	/**
	 * Select multiple distinct columns.
	 * <p>
	 * Creates "SELECT DISTINCT {@code columns} ..." query builder where duplicate rows
	 * are removed from the set of result rows. For the purposes of detecting duplicate
	 * rows, two NULL values are considered to be equal.
	 *
	 * @param columns Columns to select. These params must be one of annotation processor
	 *                generated column objects that corresponds to column in a database
	 *                table
	 * @return SQL SELECT statement builder
	 */
	@CheckResult
	public static Columns distinct(@NonNull @Size(min = 1) Column<?, ?, ?, ?>... columns) {
		return new Columns(new Select<SelectN>("SELECT DISTINCT"), columns);
	}

	/**
	 * Select all columns from {@code table}.
	 * <p>
	 * Creates "SELECT * FROM {@code table} ..." query builder.
	 *
	 * @param table Table to select from. This param must be one of annotation processor
	 *              generated table objects that corresponds to table in a database
	 * @param <T>   Java type that table represents
	 * @return SQL SELECT statement builder
	 */
	@CheckResult
	public static <T> From<T, T, SelectN> from(@NonNull Table<T> table) {
		return new From<>(all(), table);
	}

	/**
	 * Create raw SQL select statement.
	 *
	 * @param sql SQL SELECT statement
	 * @return Raw SQL SELECT statement builder
	 */
	@CheckResult
	public static RawSelect raw(@NonNull String sql) {
		return new RawSelect(sql);
	}

	/**
	 * Builder for SQL SELECT statement.
	 *
	 * @param <R> Selection return type
	 */
	public static final class SingleColumn<R> extends SelectSqlNode<Select1> {
		@NonNull
		final Column<?, R, ?, ?> column;

		SingleColumn(@Nullable SelectSqlNode<Select1> parent, @NonNull Column<?, R, ?, ?> column) {
			super(parent);
			this.column = column;
			selectBuilder.columnNode = this;
			// deep, so we could select any column
			selectBuilder.deep = true;
			column.addArgs(selectBuilder.args);
			column.addObservedTables(selectBuilder.observedTables);
		}

		@NonNull
		StringArraySet preCompileColumns() {
			final StringArraySet selectFromTables = new StringArraySet(BASE_SIZE);
			column.addSelectedTables(selectFromTables);
			return selectFromTables;
		}

		@Override
		void appendSql(@NonNull StringBuilder sb) {
			column.appendSql(sb);
			column.appendAliasDeclarationIfNeeded(sb);
		}

		@Override
		void appendSql(@NonNull StringBuilder sb, @NonNull SimpleArrayMap<String, LinkedList<String>> systemRenamedTables) {
			column.appendSql(sb, systemRenamedTables);
			column.appendAliasDeclarationIfNeeded(sb);
		}

		/**
		 * Define a FROM clause.
		 *
		 * @param table Table to select from. This param must be one of annotation processor
		 *              generated table objects that corresponds to table in a database
		 * @param <T>   Java type that table represents
		 * @return SQL SELECT statement builder
		 */
		@CheckResult
		public <T> From<T, R, Select1> from(@NonNull Table<T> table) {
			return new From<>(this, table);
		}
	}

	/**
	 * Builder for SQL SELECT statement.
	 */
	public static final class Columns extends SelectSqlNode<SelectN> {
		@NonNull
		final Column[] columns;
		String compiledColumns;

		Columns(@NonNull SelectSqlNode<SelectN> parent, @NonNull Column<?, ?, ?, ?>[] columns) {
			super(parent);
			this.columns = columns;
			selectBuilder.columnsNode = this;
			final ArrayList<String> args = selectBuilder.args;
			final ArrayList<String> observedTables = selectBuilder.observedTables;
			for (int i = 0, length = columns.length; i < length; i++) {
				final Column<?, ?, ?, ?> column = columns[i];
				column.addArgs(args);
				column.addObservedTables(observedTables);
			}
		}

		/**
		 * Compiles columns before anything else is built.
		 * <p>
		 * This method determines what tables are selected.
		 *
		 * @return Tables that are selected in the statement (determined by the selected columns).
		 * If null or empty then select is from all needed tables.
		 */
		@Nullable
		StringArraySet preCompileColumns() {
			final Column[] columns = this.columns;
			final int columnsCount = columns.length;
			if (columnsCount > 0) {
				final StringArraySet selectFromTables = new StringArraySet(columnsCount);
				for (int i = 0; i < columnsCount; i++) {
					columns[i].addSelectedTables(selectFromTables);
				}
				return selectFromTables;
			}
			return null;
		}

		@NonNull
		SimpleArrayMap<String, Integer> compileColumns(@Nullable SimpleArrayMap<String, LinkedList<String>> systemRenamedTables) {
			final Column[] columns = this.columns;
			final int length = columns.length;
			if (length == 0) {
				this.compiledColumns = "*";
				return new SimpleArrayMap<>();
			}
			final SimpleArrayMap<String, Integer> columnPositions = new SimpleArrayMap<>(length);
			final StringBuilder compiledCols = new StringBuilder(length * 12);
			int columnOffset = 0;
			boolean first = true;
			for (int i = 0; i < length; i++) {
				if (first) {
					first = false;
				} else {
					compiledCols.append(',');
				}
				if (systemRenamedTables != null) {
					columnOffset = columns[i].compile(columnPositions, compiledCols, systemRenamedTables, columnOffset);
				} else {
					columnOffset = columns[i].compile(columnPositions, compiledCols, columnOffset);
				}
			}
			this.compiledColumns = compiledCols.toString();
			return columnPositions;
		}

		@Override
		void appendSql(@NonNull StringBuilder sb) {
			sb.append(compiledColumns);
		}

		@Override
		void appendSql(@NonNull StringBuilder sb, @NonNull SimpleArrayMap<String, LinkedList<String>> systemRenamedTables) {
			sb.append(compiledColumns);
		}

		/**
		 * Define a FROM clause.
		 *
		 * @param table Table to select from. This param must be one of annotation processor
		 *              generated table objects that corresponds to table in a database
		 * @param <T>   Java type that table represents
		 * @return SQL SELECT statement builder
		 */
		@CheckResult
		public <T> From<T, T, SelectN> from(@NonNull Table<T> table) {
			return new From<>(this, table);
		}
	}

	/**
	 * Builder for SQL SELECT statement.
	 *
	 * @param <T> Selected table type
	 * @param <R> Selection return type
	 * @param <S> Selection type -- either {@link Select1} or {@link SelectN}
	 */
	public static final class From<T, R, S> extends SelectNode<R, S> {
		private static final String COMMA_JOIN = ",";
		static final String LEFT_JOIN = "LEFT JOIN";
		private static final String LEFT_OUTER_JOIN = "LEFT OUTER JOIN";
		private static final String INNER_JOIN = "INNER JOIN";
		private static final String CROSS_JOIN = "CROSS JOIN";
		private static final String NATURAL_JOIN = "NATURAL JOIN";
		private static final String NATURAL_LEFT_JOIN = "NATURAL LEFT JOIN";
		private static final String NATURAL_LEFT_OUTER_JOIN = "NATURAL LEFT OUTER JOIN";
		private static final String NATURAL_INNER_JOIN = "NATURAL INNER JOIN";
		private static final String NATURAL_CROSS_JOIN = "NATURAL CROSS JOIN";

		@NonNull
		final Table<T> table;
		final ArrayList<JoinClause> joins = new ArrayList<>();

		From(@NonNull SelectSqlNode<S> parent, @NonNull Table<T> table) {
			super(parent);
			this.table = table;
			selectBuilder.from = this;
		}

		@Override
		void appendSql(@NonNull StringBuilder sb) {
			sb.append("FROM ");
			table.appendToSqlFromClause(sb);
			final ArrayList<JoinClause> joins = this.joins;
			for (int i = 0, size = joins.size(); i < size; i++) {
				sb.append(' ');
				joins.get(i).appendSql(sb);
			}
		}

		@Override
		void appendSql(@NonNull StringBuilder sb, @NonNull SimpleArrayMap<String, LinkedList<String>> systemRenamedTables) {
			sb.append("FROM ");
			table.appendToSqlFromClause(sb);
			final ArrayList<JoinClause> joins = this.joins;
			for (int i = 0, size = joins.size(); i < size; i++) {
				sb.append(' ');
				joins.get(i).appendSql(sb, systemRenamedTables);
			}
		}

		/**
		 * Join a table to the selected table using the comma (",") join-operator.
		 * <p>
		 * Joined table can only be a complex column of the selected table. Any other table join
		 * will be ignored in the resulting SQL.
		 *
		 * @param table Table to join. This param must be one of annotation processor
		 *              generated table objects that corresponds to table in a database
		 * @return SQL SELECT statement builder
		 */
		@CheckResult
		public From<T, R, S> join(@NonNull Table table) {
			joins.add(new JoinClause(table, COMMA_JOIN, null));
			return this;
		}

		/**
		 * Join a table with ON or USING clause to the selected table using the
		 * comma (",") join-operator.
		 * <p>
		 * Joined table can only be a complex column of the selected table. Any other table join
		 * will be ignored in the resulting SQL.
		 *
		 * @param joinClause Join clause to use. This param is the result of
		 *                   {@link Table#on(Expr) ON} or {@link Table#using(Column[]) USING}
		 *                   clauses invoked on one of annotation processor generated
		 *                   table objects that corresponds to table in a database
		 * @return SQL SELECT statement builder
		 */
		@CheckResult
		public From<T, R, S> join(@NonNull JoinClause joinClause) {
			joinClause.operator = COMMA_JOIN;
			joins.add(joinClause);
			joinClause.addArgs(selectBuilder.args);
			return this;
		}

		/**
		 * LEFT JOIN a table to the selected table.
		 * <p>
		 * Joined table can only be a complex column of the selected table. Any other table join
		 * will be ignored in the resulting SQL.
		 *
		 * @param table Table to join. This param must be one of annotation processor
		 *              generated table objects that corresponds to table in a database
		 * @return SQL SELECT statement builder
		 */
		@CheckResult
		public From<T, R, S> leftJoin(@NonNull Table table) {
			joins.add(new JoinClause(table, LEFT_JOIN, null));
			return this;
		}

		/**
		 * LEFT JOIN a table with ON or USING clause to the selected table.
		 * <p>
		 * Joined table can only be a complex column of the selected table. Any other table join
		 * will be ignored in the resulting SQL.
		 *
		 * @param joinClause Join clause to use. This param is the result of
		 *                   {@link Table#on(Expr) ON} or {@link Table#using(Column[]) USING}
		 *                   clauses invoked on one of annotation processor generated
		 *                   table objects that corresponds to table in a database
		 * @return SQL SELECT statement builder
		 */
		@CheckResult
		public From<T, R, S> leftJoin(@NonNull JoinClause joinClause) {
			joinClause.operator = LEFT_JOIN;
			joins.add(joinClause);
			joinClause.addArgs(selectBuilder.args);
			return this;
		}

		/**
		 * LEFT OUTER JOIN a table to the selected table.
		 * <p>
		 * Joined table can only be a complex column of the selected table. Any other table join
		 * will be ignored in the resulting SQL.
		 *
		 * @param table Table to join. This param must be one of annotation processor
		 *              generated table objects that corresponds to table in a database
		 * @return SQL SELECT statement builder
		 */
		@CheckResult
		public From<T, R, S> leftOuterJoin(@NonNull Table table) {
			joins.add(new JoinClause(table, LEFT_OUTER_JOIN, null));
			return this;
		}

		/**
		 * LEFT OUTER JOIN a table with ON or USING clause to the selected table.
		 * <p>
		 * Joined table can only be a complex column of the selected table. Any other table join
		 * will be ignored in the resulting SQL.
		 *
		 * @param joinClause Join clause to use. This param is the result of
		 *                   {@link Table#on(Expr) ON} or {@link Table#using(Column[]) USING}
		 *                   clauses invoked on one of annotation processor generated
		 *                   table objects that corresponds to table in a database
		 * @return SQL SELECT statement builder
		 */
		@CheckResult
		public From<T, R, S> leftOuterJoin(@NonNull JoinClause joinClause) {
			joinClause.operator = LEFT_OUTER_JOIN;
			joins.add(joinClause);
			joinClause.addArgs(selectBuilder.args);
			return this;
		}

		/**
		 * INNER JOIN a table to the selected table.
		 * <p>
		 * Joined table can only be a complex column of the selected table. Any other table join
		 * will be ignored in the resulting SQL.
		 *
		 * @param table Table to join. This param must be one of annotation processor
		 *              generated table objects that corresponds to table in a database
		 * @return SQL SELECT statement builder
		 */
		@CheckResult
		public From<T, R, S> innerJoin(@NonNull Table table) {
			joins.add(new JoinClause(table, INNER_JOIN, null));
			return this;
		}

		/**
		 * INNER JOIN a table with ON or USING clause to the selected table.
		 * <p>
		 * Joined table can only be a complex column of the selected table. Any other table join
		 * will be ignored in the resulting SQL.
		 *
		 * @param joinClause Join clause to use. This param is the result of
		 *                   {@link Table#on(Expr) ON} or {@link Table#using(Column[]) USING}
		 *                   clauses invoked on one of annotation processor generated
		 *                   table objects that corresponds to table in a database
		 * @return SQL SELECT statement builder
		 */
		@CheckResult
		public From<T, R, S> innerJoin(@NonNull JoinClause joinClause) {
			joinClause.operator = INNER_JOIN;
			joins.add(joinClause);
			joinClause.addArgs(selectBuilder.args);
			return this;
		}

		/**
		 * CROSS JOIN a table to the selected table.
		 * <p>
		 * Joined table can only be a complex column of the selected table. Any other table join
		 * will be ignored in the resulting SQL.
		 *
		 * @param table Table to join. This param must be one of annotation processor
		 *              generated table objects that corresponds to table in a database
		 * @return SQL SELECT statement builder
		 */
		@CheckResult
		public From<T, R, S> crossJoin(@NonNull Table table) {
			joins.add(new JoinClause(table, CROSS_JOIN, null));
			return this;
		}

		/**
		 * CROSS JOIN a table with ON or USING clause to the selected table.
		 * <p>
		 * Joined table can only be a complex column of the selected table. Any other table join
		 * will be ignored in the resulting SQL.
		 *
		 * @param joinClause Join clause to use. This param is the result of
		 *                   {@link Table#on(Expr) ON} or {@link Table#using(Column[]) USING}
		 *                   clauses invoked on one of annotation processor generated
		 *                   table objects that corresponds to table in a database
		 * @return SQL SELECT statement builder
		 */

		@CheckResult
		public From<T, R, S> crossJoin(@NonNull JoinClause joinClause) {
			joinClause.operator = CROSS_JOIN;
			joins.add(joinClause);
			joinClause.addArgs(selectBuilder.args);
			return this;
		}

		/**
		 * NATURAL JOIN a table to the selected table.
		 * <p>
		 * Joined table can only be a complex column of the selected table. Any other table join
		 * will be ignored in the resulting SQL.
		 * <p>
		 * If the NATURAL keyword is in the join-operator then an implicit USING clause
		 * is added to the join-constraints. The implicit USING clause contains each of
		 * the column names that appear in both the left and right-hand input datasets.
		 * If the left and right-hand input datasets feature no common column names, then
		 * the NATURAL keyword has no effect on the results of the join.
		 *
		 * @param table Table to join. This param must be one of annotation processor
		 *              generated table objects that corresponds to table in a database
		 * @return SQL SELECT statement builder
		 */
		@CheckResult
		public From<T, R, S> naturalJoin(@NonNull Table table) {
			joins.add(new JoinClause(table, NATURAL_JOIN, null));
			return this;
		}

		/**
		 * NATURAL LEFT JOIN a table to the selected table.
		 * <p>
		 * Joined table can only be a complex column of the selected table. Any other table join
		 * will be ignored in the resulting SQL.
		 * <p>
		 * If the NATURAL keyword is in the join-operator then an implicit USING clause
		 * is added to the join-constraints. The implicit USING clause contains each of
		 * the column names that appear in both the left and right-hand input datasets.
		 * If the left and right-hand input datasets feature no common column names, then
		 * the NATURAL keyword has no effect on the results of the join.
		 *
		 * @param table Table to join. This param must be one of annotation processor
		 *              generated table objects that corresponds to table in a database
		 * @return SQL SELECT statement builder
		 */
		@CheckResult
		public From<T, R, S> naturalLeftJoin(@NonNull Table table) {
			joins.add(new JoinClause(table, NATURAL_LEFT_JOIN, null));
			return this;
		}

		/**
		 * NATURAL LEFT OUTER JOIN a table to the selected table.
		 * <p>
		 * Joined table can only be a complex column of the selected table. Any other table join
		 * will be ignored in the resulting SQL.
		 * <p>
		 * If the NATURAL keyword is in the join-operator then an implicit USING clause
		 * is added to the join-constraints. The implicit USING clause contains each of
		 * the column names that appear in both the left and right-hand input datasets.
		 * If the left and right-hand input datasets feature no common column names, then
		 * the NATURAL keyword has no effect on the results of the join.
		 *
		 * @param table Table to join. This param must be one of annotation processor
		 *              generated table objects that corresponds to table in a database
		 * @return SQL SELECT statement builder
		 */
		@CheckResult
		public From<T, R, S> naturalLeftOuterJoin(@NonNull Table table) {
			joins.add(new JoinClause(table, NATURAL_LEFT_OUTER_JOIN, null));
			return this;
		}

		/**
		 * NATURAL INNER JOIN a table to the selected table.
		 * <p>
		 * Joined table can only be a complex column of the selected table. Any other table join
		 * will be ignored in the resulting SQL.
		 * <p>
		 * If the NATURAL keyword is in the join-operator then an implicit USING clause
		 * is added to the join-constraints. The implicit USING clause contains each of
		 * the column names that appear in both the left and right-hand input datasets.
		 * If the left and right-hand input datasets feature no common column names, then
		 * the NATURAL keyword has no effect on the results of the join.
		 *
		 * @param table Table to join. This param must be one of annotation processor
		 *              generated table objects that corresponds to table in a database
		 * @return SQL SELECT statement builder
		 */
		@CheckResult
		public From<T, R, S> naturalInnerJoin(@NonNull Table table) {
			joins.add(new JoinClause(table, NATURAL_INNER_JOIN, null));
			return this;
		}

		/**
		 * NATURAL CROSS JOIN a table to the selected table.
		 * <p>
		 * Joined table can only be a complex column of the selected table. Any other table join
		 * will be ignored in the resulting SQL.
		 * <p>
		 * If the NATURAL keyword is in the join-operator then an implicit USING clause
		 * is added to the join-constraints. The implicit USING clause contains each of
		 * the column names that appear in both the left and right-hand input datasets.
		 * If the left and right-hand input datasets feature no common column names, then
		 * the NATURAL keyword has no effect on the results of the join.
		 *
		 * @param table Table to join. This param must be one of annotation processor
		 *              generated table objects that corresponds to table in a database
		 * @return SQL SELECT statement builder
		 */
		@CheckResult
		public From<T, R, S> naturalCrossJoin(@NonNull Table table) {
			joins.add(new JoinClause(table, NATURAL_CROSS_JOIN, null));
			return this;
		}

		/**
		 * Define a WHERE clause.
		 *
		 * @param expr WHERE clause expression
		 * @return SQL SELECT statement builder
		 */
		@CheckResult
		public Where<R, S> where(@NonNull Expr expr) {
			return new Where<>(this, expr);
		}

		/**
		 * Add a GROUP BY clause to the query.
		 *
		 * @param columns Columns to group selection by. These params must be one of
		 *                annotation processor generated column objects that corresponds
		 *                to column in a database table
		 * @return SQL SELECT statement builder
		 */
		@CheckResult
		public GroupBy<R, S> groupBy(@NonNull @Size(min = 1) Column... columns) {
			return new GroupBy<>(this, columns);
		}

		/**
		 * Add an ORDER BY clause to the query.
		 *
		 * @param orderingTerm Ordering term that defines ORDER BY clause.
		 * @return SQL SELECT statement builder
		 */
		@CheckResult
		public OrderBy<R, S> order(@NonNull OrderingTerm orderingTerm) {
			return new OrderBy<>(this, orderingTerm);
		}

		/**
		 * Add a LIMIT clause to the query.
		 *
		 * @param nrOfRows Upper bound on the number of rows returned by the
		 *                 entire SELECT statement
		 * @return SQL SELECT statement builder
		 */
		@CheckResult
		public Limit<R, S> limit(int nrOfRows) {
			return new Limit<>(this, Integer.toString(nrOfRows));
		}
	}

	/**
	 * Builder for SQL SELECT statement.
	 *
	 * @param <T> Selection return type
	 * @param <S> Selection type -- either {@link Select1} or {@link SelectN}
	 */
	public static final class Where<T, S> extends SelectNode<T, S> {
		@NonNull
		private final Expr expr;

		Where(@NonNull SelectNode<T, S> parent, @NonNull Expr expr) {
			super(parent);
			this.expr = expr;
			expr.addArgs(selectBuilder.args);
			expr.addObservedTables(selectBuilder.observedTables);
		}

		@Override
		void appendSql(@NonNull StringBuilder sb) {
			sb.append("WHERE ");
			expr.appendToSql(sb);
		}

		@Override
		void appendSql(@NonNull StringBuilder sb, @NonNull SimpleArrayMap<String, LinkedList<String>> systemRenamedTables) {
			sb.append("WHERE ");
			expr.appendToSql(sb, systemRenamedTables);
		}

		/**
		 * Add a GROUP BY clause to the query.
		 *
		 * @param columns Columns to group selection by. These params must be one of
		 *                annotation processor generated column objects that corresponds
		 *                to column in a database table
		 * @return SQL SELECT statement builder
		 */
		@CheckResult
		public GroupBy<T, S> groupBy(@NonNull @Size(min = 1) Column... columns) {
			return new GroupBy<>(this, columns);
		}

		/**
		 * Add an ORDER BY clause to the query.
		 *
		 * @param orderingTerm Ordering term that defines ORDER BY clause.
		 * @return SQL SELECT statement builder
		 */
		@CheckResult
		public OrderBy<T, S> order(@NonNull OrderingTerm orderingTerm) {
			return new OrderBy<>(this, orderingTerm);
		}

		/**
		 * Add a LIMIT clause to the query.
		 *
		 * @param nrOfRows Upper bound on the number of rows returned by the
		 *                 entire SELECT statement
		 * @return SQL SELECT statement builder
		 */
		@CheckResult
		public Limit<T, S> limit(int nrOfRows) {
			return new Limit<>(this, Integer.toString(nrOfRows));
		}
	}

	/**
	 * Builder for SQL SELECT statement.
	 *
	 * @param <T> Selection return type
	 * @param <S> Selection type -- either {@link Select1} or {@link SelectN}
	 */
	public static final class GroupBy<T, S> extends SelectNode<T, S> {
		@NonNull
		private final Column[] columns;

		GroupBy(@NonNull SelectNode<T, S> parent, @NonNull Column[] columns) {
			super(parent);
			this.columns = columns;
		}

		@Override
		void appendSql(@NonNull StringBuilder sb) {
			sb.append("GROUP BY ");
			appendColumns(sb, columns);
		}

		@Override
		void appendSql(@NonNull StringBuilder sb, @NonNull SimpleArrayMap<String, LinkedList<String>> systemRenamedTables) {
			sb.append("GROUP BY ");
			appendColumns(sb, columns, systemRenamedTables);
		}

		/**
		 * Add a HAVING clause to the query.
		 *
		 * @param expr HAVING clause expression
		 * @return SQL SELECT statement builder
		 */
		@CheckResult
		public Having<T, S> having(@NonNull Expr expr) {
			return new Having<>(this, expr);
		}

		/**
		 * Add an ORDER BY clause to the query.
		 *
		 * @param orderingTerm Ordering term that defines ORDER BY clause.
		 * @return SQL SELECT statement builder
		 */
		@CheckResult
		public OrderBy<T, S> order(@NonNull OrderingTerm orderingTerm) {
			return new OrderBy<>(this, orderingTerm);
		}

		/**
		 * Add a LIMIT clause to the query.
		 *
		 * @param nrOfRows Upper bound on the number of rows returned by the
		 *                 entire SELECT statement
		 * @return SQL SELECT statement builder
		 */
		@CheckResult
		public Limit<T, S> limit(int nrOfRows) {
			return new Limit<>(this, Integer.toString(nrOfRows));
		}
	}

	/**
	 * Builder for SQL SELECT statement.
	 *
	 * @param <T> Selection return type
	 * @param <S> Selection type -- either {@link Select1} or {@link SelectN}
	 */
	public static final class Having<T, S> extends SelectNode<T, S> {
		@NonNull
		private final Expr expr;

		Having(@NonNull SelectNode<T, S> parent, @NonNull Expr expr) {
			super(parent);
			this.expr = expr;
			expr.addArgs(selectBuilder.args);
			expr.addObservedTables(selectBuilder.observedTables);
		}

		@Override
		void appendSql(@NonNull StringBuilder sb) {
			sb.append("HAVING ");
			expr.appendToSql(sb);
		}

		@Override
		void appendSql(@NonNull StringBuilder sb, @NonNull SimpleArrayMap<String, LinkedList<String>> systemRenamedTables) {
			sb.append("HAVING ");
			expr.appendToSql(sb, systemRenamedTables);
		}

		/**
		 * Add an ORDER BY clause to the query.
		 *
		 * @param orderingTerm Ordering term that defines ORDER BY clause.
		 * @return SQL SELECT statement builder
		 */
		@CheckResult
		public OrderBy<T, S> order(@NonNull OrderingTerm orderingTerm) {
			return new OrderBy<>(this, orderingTerm);
		}

		/**
		 * Add a LIMIT clause to the query.
		 *
		 * @param nrOfRows Upper bound on the number of rows returned by the
		 *                 entire SELECT statement
		 * @return SQL SELECT statement builder
		 */
		@CheckResult
		public Limit<T, S> limit(int nrOfRows) {
			return new Limit<>(this, Integer.toString(nrOfRows));
		}
	}

	/**
	 * Builder for ORDER BY ordering term.
	 */
	public static final class OrderingTerm extends SqlClause {
		@NonNull
		private final Column[] columns;
		@Nullable
		private String ordering;

		OrderingTerm(@NonNull @Size(min = 1) Column[] columns) {
			this.columns = columns;
		}

		/**
		 * Create ORDER BY ordering term.
		 *
		 * @param columns Columns to order selection by. These params must be one of
		 *                annotation processor generated column objects that corresponds
		 *                to column in a database table
		 * @return Ordering term to be used in {@code order} method.
		 */
		@CheckResult
		public static OrderingTerm by(@NonNull @Size(min = 1) Column... columns) {
			return new OrderingTerm(columns);
		}

		/**
		 * Order rows in ascending order.
		 *
		 * @return Ordering term to be used in {@code order} method.
		 */
		@CheckResult
		public OrderingTerm asc() {
			ordering = " ASC";
			return this;
		}

		/**
		 * Order rows in descending order.
		 *
		 * @return Ordering term to be used in {@code order} method.
		 */
		@CheckResult
		public OrderingTerm desc() {
			ordering = " DESC";
			return this;
		}

		@Override
		void appendSql(@NonNull StringBuilder sb) {
			appendColumns(sb, columns);
			if (ordering != null) {
				sb.append(ordering);
			}
		}

		@Override
		void appendSql(@NonNull StringBuilder sb, @NonNull SimpleArrayMap<String, LinkedList<String>> systemRenamedTables) {
			appendColumns(sb, columns, systemRenamedTables);
			if (ordering != null) {
				sb.append(ordering);
			}
		}
	}

	/**
	 * Builder for SQL SELECT statement.
	 *
	 * @param <T> Selection return type
	 * @param <S> Selection type -- either {@link Select1} or {@link SelectN}
	 */
	public static final class OrderBy<T, S> extends SelectNode<T, S> {
		@NonNull
		private final OrderingTerm orderingTerm;

		OrderBy(@NonNull SelectNode<T, S> parent, @NonNull OrderingTerm orderingTerm) {
			super(parent);
			this.orderingTerm = orderingTerm;
		}

		@Override
		void appendSql(@NonNull StringBuilder sb) {
			sb.append("ORDER BY ");
			orderingTerm.appendSql(sb);
		}

		@Override
		void appendSql(@NonNull StringBuilder sb, @NonNull SimpleArrayMap<String, LinkedList<String>> systemRenamedTables) {
			sb.append("ORDER BY ");
			orderingTerm.appendSql(sb, systemRenamedTables);
		}

		/**
		 * Add a LIMIT clause to the query.
		 *
		 * @param nrOfRows Upper bound on the number of rows returned by the
		 *                 entire SELECT statement
		 * @return SQL SELECT statement builder
		 */
		@CheckResult
		public Limit<T, S> limit(int nrOfRows) {
			return new Limit<>(this, Integer.toString(nrOfRows));
		}
	}

	/**
	 * Builder for SQL SELECT statement.
	 *
	 * @param <T> Selection return type
	 * @param <S> Selection type -- either {@link Select1} or {@link SelectN}
	 */
	public static final class Limit<T, S> extends SelectNode<T, S> {
		private final String limitClause;

		Limit(@NonNull SelectNode<T, S> parent, @NonNull String limitClause) {
			super(parent);
			this.limitClause = limitClause;
		}

		@Override
		void appendSql(@NonNull StringBuilder sb) {
			sb.append("LIMIT ")
					.append(limitClause);
		}

		@Override
		void appendSql(@NonNull StringBuilder sb, @NonNull SimpleArrayMap<String, LinkedList<String>> systemRenamedTables) {
			sb.append("LIMIT ")
					.append(limitClause);
		}

		@CheckResult
		public Offset<T, S> offset(int rowNr) {
			return new Offset<>(this, String.valueOf(rowNr));
		}
	}

	/**
	 * Builder for SQL SELECT statement.
	 *
	 * @param <T> Selection return type
	 * @param <S> Selection type -- either {@link Select1} or {@link SelectN}
	 */
	public static final class Offset<T, S> extends SelectNode<T, S> {
		private final String offsetClause;

		Offset(@NonNull SelectNode<T, S> parent, @NonNull String offsetClause) {
			super(parent);
			this.offsetClause = offsetClause;
		}

		@Override
		void appendSql(@NonNull StringBuilder sb) {
			sb.append("OFFSET ")
					.append(offsetClause);
		}

		@Override
		void appendSql(@NonNull StringBuilder sb, @NonNull SimpleArrayMap<String, LinkedList<String>> systemRenamedTables) {
			sb.append("OFFSET ")
					.append(offsetClause);
		}
	}

	static void appendColumns(@NonNull StringBuilder sb, @NonNull Column[] columns) {
		for (int i = 0, length = columns.length; i < length; i++) {
			if (i > 0) {
				sb.append(',');
			}
			columns[i].appendSql(sb);
		}
	}

	static void appendColumns(@NonNull StringBuilder sb, @NonNull Column[] columns, @NonNull SimpleArrayMap<String, LinkedList<String>> systemRenamedTables) {
		for (int i = 0, length = columns.length; i < length; i++) {
			if (i > 0) {
				sb.append(',');
			}
			columns[i].appendSql(sb, systemRenamedTables);
		}
	}

	/* ###############################################################################
	 * #################################  FUNCTIONS  #################################
	 * ###############################################################################
	 */

	private static final NumericColumn<Long, Long, Number, ?> COUNT = new NumericColumn<>(ANONYMOUS_TABLE, "count(*)", false, LONG_PARSER, false, null);

	/**
	 * <p>
	 * The avg() function returns the average value of all non-NULL X within a group.
	 * String and BLOB values that do not look like numbers are interpreted as 0.
	 * </p>
	 * The result of avg() is always a floating point value as long as at there is at least
	 * one non-NULL input even if all inputs are integers.
	 * The result of avg() is NULL if and only if there are no non-NULL inputs.
	 *
	 * @param column Input of this aggregate function
	 * @return Column representing the result of this function
	 * @see <a href="http://www.sqlite.org/lang_aggfunc.html">SQLite documentation: Aggregate Functions</a>
	 */
	@NonNull
	@CheckResult
	public static <P, X extends NumericColumn<?, ?, ? extends Number, P>> NumericColumn<Double, Double, Number, P> avg(@NonNull X column) {
		return new FunctionColumn<>(column.table.internalAlias(""), column, "avg(", ")", DOUBLE_PARSER, true, null);
	}

	/**
	 * <p>
	 * The avg() function returns the average value of distinct values of column X.
	 * String and BLOB values that do not look like numbers are interpreted as 0.
	 * </p>
	 * <p>
	 * Duplicate elements are filtered before being passed into the aggregate function.
	 * </p>
	 * The result of avg() is always a floating point value as long as at there is at least
	 * one non-NULL input even if all inputs are integers.
	 * The result of avg() is NULL if and only if there are no non-NULL inputs.
	 *
	 * @param column Input of this aggregate function
	 * @return Column representing the result of this function
	 * @see <a href="http://www.sqlite.org/lang_aggfunc.html">SQLite documentation: Aggregate Functions</a>
	 */
	@NonNull
	@CheckResult
	public static <P, X extends NumericColumn<?, ?, ? extends Number, P>> NumericColumn<Double, Double, Number, P> avgDistinct(@NonNull X column) {
		return new FunctionColumn<>(column.table.internalAlias(""), column, "avg(DISTINCT ", ")", DOUBLE_PARSER, true, null);
	}

	/**
	 * Function returns the total number of rows in the group.
	 *
	 * @return Column representing the result of this function
	 * @see <a href="http://www.sqlite.org/lang_aggfunc.html">SQLite documentation: Aggregate Functions</a>
	 */
	@NonNull
	@CheckResult
	public static NumericColumn<Long, Long, Number, ?> count() {
		return COUNT;
	}

	/**
	 * The count(X) function returns a count of the number of times that X is not NULL in a group.
	 *
	 * @param column Input of this aggregate function
	 * @return Column representing the result of this function
	 * @see <a href="http://www.sqlite.org/lang_aggfunc.html">SQLite documentation: Aggregate Functions</a>
	 */
	@NonNull
	@CheckResult
	public static <P, X extends Column<?, ?, ?, P>> NumericColumn<Long, Long, Number, P> count(@NonNull X column) {
		return new FunctionColumn<>(column.table.internalAlias(""), column, "count(", ")", LONG_PARSER, false, null);
	}

	/**
	 * The count(DISTINCT X) function returns the number of distinct values of column X.
	 *
	 * @param column Input of this aggregate function
	 * @return Column representing the result of this function
	 * @see <a href="http://www.sqlite.org/lang_aggfunc.html">SQLite documentation: Aggregate Functions</a>
	 */
	@NonNull
	@CheckResult
	public static <P, X extends Column<?, ?, ?, P>> NumericColumn<Long, Long, Number, P> countDistinct(@NonNull X column) {
		return new FunctionColumn<>(column.table.internalAlias(""), column, "count(DISTINCT ", ")", LONG_PARSER, false, null);
	}

	/**
	 * The group_concat() function returns a string which is the concatenation of all non-NULL values of X.
	 * A comma (",") is used as the separator. The order of the concatenated elements is arbitrary.
	 *
	 * @param column Input of this aggregate function
	 * @return Column representing the result of this function
	 * @see <a href="http://www.sqlite.org/lang_aggfunc.html">SQLite documentation: Aggregate Functions</a>
	 */
	@NonNull
	@CheckResult
	public static <P, X extends Column<?, ?, ?, P>> Column<String, String, CharSequence, P> groupConcat(@NonNull X column) {
		return new FunctionColumn<>(column.table.internalAlias(""), column, "group_concat(", ")", STRING_PARSER, false, null);
	}

	/**
	 * The group_concat() function returns a string which is the concatenation of all non-NULL values of X.
	 * Parameter "separator" is used as the separator between instances of X.
	 * The order of the concatenated elements is arbitrary.
	 *
	 * @param column    Input of this aggregate function
	 * @param separator Separator between instances of X
	 * @return Column representing the result of this function
	 * @see <a href="http://www.sqlite.org/lang_aggfunc.html">SQLite documentation: Aggregate Functions</a>
	 */
	@NonNull
	@CheckResult
	public static <P, X extends Column<?, ?, ?, P>> Column<String, String, CharSequence, P> groupConcat(@NonNull X column, @NonNull String separator) {
		return new FunctionColumn<>(column.table.internalAlias(""), column, "group_concat(", ",'" + separator + "')", STRING_PARSER, false, null);
	}

	/**
	 * The group_concat() function returns a string which is the concatenation of distinct values of column X.
	 * A comma (",") is used as the separator. The order of the concatenated elements is arbitrary.
	 *
	 * @param column Input of this aggregate function
	 * @return Column representing the result of this function
	 * @see <a href="http://www.sqlite.org/lang_aggfunc.html">SQLite documentation: Aggregate Functions</a>
	 */
	@NonNull
	@CheckResult
	public static <P, X extends Column<?, ?, ?, P>> Column<String, String, CharSequence, P> groupConcatDistinct(@NonNull X column) {
		return new FunctionColumn<>(column.table.internalAlias(""), column, "group_concat(DISTINCT ", ")", STRING_PARSER, false, null);
	}

	/**
	 * The max() aggregate function returns the maximum value of all values in the group.
	 * The maximum value is the value that would be returned last in an ORDER BY on the same column.
	 * Aggregate max() returns NULL if and only if there are no non-NULL values in the group.
	 *
	 * @param column Input of this aggregate function
	 * @return Column representing the result of this function
	 * @see <a href="http://www.sqlite.org/lang_aggfunc.html">SQLite documentation: Aggregate Functions</a>
	 */
	@NonNull
	@CheckResult
	public static <P, T, R, ET, X extends Column<T, R, ET, P>> Column<T, R, ET, P> max(@NonNull X column) {
		return new FunctionCopyColumn<>(column.table.internalAlias(""), column, "max(", ')', true, null);
	}

	/**
	 * The max() aggregate function returns the maximum value of all values in the group.
	 * The maximum value is the value that would be returned last in an ORDER BY on the same column.
	 * Aggregate max() returns NULL if and only if there are no non-NULL values in the group.
	 *
	 * @param column Input of this aggregate function
	 * @return Column representing the result of this function
	 * @see <a href="http://www.sqlite.org/lang_aggfunc.html">SQLite documentation: Aggregate Functions</a>
	 */
	@NonNull
	@CheckResult
	public static <P, T, R, ET, X extends NumericColumn<T, R, ET, P>> NumericColumn<T, R, ET, P> max(@NonNull X column) {
		return new FunctionCopyColumn<>(column.table.internalAlias(""), column, "max(", ')', true, null);
	}

	/**
	 * The max() aggregate function returns the maximum value of distinct values of column X.
	 * The maximum value is the value that would be returned last in an ORDER BY on the same column.
	 * Aggregate max() returns NULL if and only if there are no non-NULL values in the group.
	 *
	 * @param column Input of this aggregate function
	 * @return Column representing the result of this function
	 * @see <a href="http://www.sqlite.org/lang_aggfunc.html">SQLite documentation: Aggregate Functions</a>
	 */
	@NonNull
	@CheckResult
	public static <P, T, R, ET, X extends Column<T, R, ET, P>> Column<T, R, ET, P> maxDistinct(@NonNull X column) {
		return new FunctionCopyColumn<>(column.table.internalAlias(""), column, "max(DISTINCT ", ')', true, null);
	}

	/**
	 * The max() aggregate function returns the maximum value of distinct values of column X.
	 * The maximum value is the value that would be returned last in an ORDER BY on the same column.
	 * Aggregate max() returns NULL if and only if there are no non-NULL values in the group.
	 *
	 * @param column Input of this aggregate function
	 * @return Column representing the result of this function
	 * @see <a href="http://www.sqlite.org/lang_aggfunc.html">SQLite documentation: Aggregate Functions</a>
	 */
	@NonNull
	@CheckResult
	public static <P, T, R, ET, X extends NumericColumn<T, R, ET, P>> NumericColumn<T, R, ET, P> maxDistinct(@NonNull X column) {
		return new FunctionCopyColumn<>(column.table.internalAlias(""), column, "max(DISTINCT ", ')', true, null);
	}

	/**
	 * The min() aggregate function returns the minimum non-NULL value of all values in the group.
	 * The minimum value is the first non-NULL value that would appear in an ORDER BY of the column.
	 * Aggregate min() returns NULL if and only if there are no non-NULL values in the group.
	 *
	 * @param column Input of this aggregate function
	 * @return Column representing the result of this function
	 * @see <a href="http://www.sqlite.org/lang_aggfunc.html">SQLite documentation: Aggregate Functions</a>
	 */
	@NonNull
	@CheckResult
	public static <P, T, R, ET, X extends Column<T, R, ET, P>> Column<T, R, ET, P> min(@NonNull X column) {
		return new FunctionCopyColumn<>(column.table.internalAlias(""), column, "min(", ')', true, null);
	}

	/**
	 * The min() aggregate function returns the minimum non-NULL value of all values in the group.
	 * The minimum value is the first non-NULL value that would appear in an ORDER BY of the column.
	 * Aggregate min() returns NULL if and only if there are no non-NULL values in the group.
	 *
	 * @param column Input of this aggregate function
	 * @return Column representing the result of this function
	 * @see <a href="http://www.sqlite.org/lang_aggfunc.html">SQLite documentation: Aggregate Functions</a>
	 */
	@NonNull
	@CheckResult
	public static <P, T, R, ET, X extends NumericColumn<T, R, ET, P>> NumericColumn<T, R, ET, P> min(@NonNull X column) {
		return new FunctionCopyColumn<>(column.table.internalAlias(""), column, "min(", ')', true, null);
	}

	/**
	 * The min() aggregate function returns the minimum value of distinct values of column X.
	 * The minimum value is the first non-NULL value that would appear in an ORDER BY of the column.
	 * Aggregate min() returns NULL if and only if there are no non-NULL values in the group.
	 *
	 * @param column Input of this aggregate function
	 * @return Column representing the result of this function
	 * @see <a href="http://www.sqlite.org/lang_aggfunc.html">SQLite documentation: Aggregate Functions</a>
	 */
	@NonNull
	@CheckResult
	public static <P, T, R, ET, X extends Column<T, R, ET, P>> Column<T, R, ET, P> minDistinct(@NonNull X column) {
		return new FunctionCopyColumn<>(column.table.internalAlias(""), column, "min(DISTINCT ", ')', true, null);
	}

	/**
	 * The min() aggregate function returns the minimum value of distinct values of column X.
	 * The minimum value is the first non-NULL value that would appear in an ORDER BY of the column.
	 * Aggregate min() returns NULL if and only if there are no non-NULL values in the group.
	 *
	 * @param column Input of this aggregate function
	 * @return Column representing the result of this function
	 * @see <a href="http://www.sqlite.org/lang_aggfunc.html">SQLite documentation: Aggregate Functions</a>
	 */
	@NonNull
	@CheckResult
	public static <P, T, R, ET, X extends NumericColumn<T, R, ET, P>> NumericColumn<T, R, ET, P> minDistinct(@NonNull X column) {
		return new FunctionCopyColumn<>(column.table.internalAlias(""), column, "min(DISTINCT ", ')', true, null);
	}

	/**
	 * <p>
	 * Sum function that uses internally total() SQLite aggregate function.
	 * </p>
	 * The function returns sum of all non-NULL values in the group. If there are no non-NULL input
	 * rows then function returns 0.0. The result of function is always a floating point value.
	 * This function never throws an integer overflow.
	 *
	 * @param column Input of this aggregate function
	 * @return Column representing the result of this function
	 * @see <a href="http://www.sqlite.org/lang_aggfunc.html">SQLite documentation: Aggregate Functions</a>
	 */
	@NonNull
	@CheckResult
	public static <P, X extends NumericColumn<?, ?, ? extends Number, P>> NumericColumn<Double, Double, Number, P> sum(@NonNull X column) {
		return new FunctionColumn<>(column.table.internalAlias(""), column, "total(", ")", DOUBLE_PARSER, false, null);
	}

	/**
	 * <p>
	 * Sum function that uses internally total() SQLite aggregate function.
	 * </p>
	 * The function returns sum of distinct values of column X. If there are no non-NULL input
	 * rows then function returns 0.0. The result of function is always a floating point value.
	 * This function never throws an integer overflow.
	 *
	 * @param column Input of this aggregate function
	 * @return Column representing the result of this function
	 * @see <a href="http://www.sqlite.org/lang_aggfunc.html">SQLite documentation: Aggregate Functions</a>
	 */
	@NonNull
	@CheckResult
	public static <P, X extends NumericColumn<?, ?, ? extends Number, P>> NumericColumn<Double, Double, Number, P> sumDistinct(@NonNull X column) {
		return new FunctionColumn<>(column.table.internalAlias(""), column, "total(DISTINCT ", ")", DOUBLE_PARSER, false, null);
	}

	/**
	 * Join columns.
	 * This operator always evaluates to either NULL or a text value.
	 *
	 * @param columns Columns to concatenate
	 * @return Column representing the result of this function
	 * @see <a href="http://www.sqlite.org/lang_expr.html">SQLite documentation: Expression</a>
	 */
	@SafeVarargs
	@NonNull
	@CheckResult
	public static <X extends Column<?, ?, ?, ?>> Column<String, String, CharSequence, ?> concat(@NonNull @Size(min = 2) X... columns) {
		return new FunctionColumn<>(ANONYMOUS_TABLE, columns, "", " || ", "", STRING_PARSER, true, null);
	}

	/**
	 * Number value as column.
	 *
	 * @param val Value
	 * @return Column representing provided value
	 */
	@NonNull
	@CheckResult
	public static <V extends Number> NumericColumn<V, V, Number, ?> val(@NonNull V val) {
		return new NumericColumn<>(ANONYMOUS_TABLE, val.toString(), false, parserForNumberType(val), false, null);
	}

	/**
	 * CharSequence value as column.
	 *
	 * @param val Value
	 * @return Column representing provided value
	 */
	@NonNull
	@CheckResult
	public static <V extends CharSequence> Column<V, V, CharSequence, ?> val(@NonNull V val) {
		return new Column<>(ANONYMOUS_TABLE, "'" + val.toString() + "'", false, STRING_PARSER, false, null);
	}

	/**
	 * Value as column.
	 *
	 * @param val Value
	 * @return Column representing provided value
	 */
	@NonNull
	@CheckResult
	public static <V> Column<V, V, V, ?> val(@NonNull V val) {
		return SqlUtil.columnForValue(val);
	}

	/**
	 * The abs(X) function returns the absolute value of the numeric argument X.
	 * Abs(X) returns NULL if X is NULL. If X is the integer -9223372036854775808 then abs(X)
	 * throws an integer overflow error since there is no equivalent positive 64-bit two complement value.
	 *
	 * @param column Input of this function
	 * @return Column representing the result of this function
	 * @see <a href="http://www.sqlite.org/lang_corefunc.html">SQLite documentation: Core Functions</a>
	 */
	@NonNull
	@CheckResult
	public static <P, T, R, ET extends Number, X extends NumericColumn<T, R, ET, P>> NumericColumn<T, R, ET, P> abs(@NonNull X column) {
		return new FunctionCopyColumn<>(column.table.internalAlias(""), column, "abs(", ')', true, null);
	}

	/**
	 * For a string value X, the length(X) function returns the number of characters (not bytes) in
	 * X prior to the first NUL character. Since SQLite strings do not normally contain NUL
	 * characters, the length(X) function will usually return the total number of characters in the
	 * string X. For a blob value X, length(X) returns the number of bytes in the blob.
	 * If X is NULL then length(X) is NULL. If X is numeric then length(X) returns the length of a
	 * string representation of X.
	 *
	 * @param column Input of this function
	 * @return Column representing the result of this function
	 * @see <a href="http://www.sqlite.org/lang_corefunc.html">SQLite documentation: Core Functions</a>
	 */
	@NonNull
	@CheckResult
	public static <P, X extends Column<?, ?, ?, P>> NumericColumn<Long, Long, Number, P> length(@NonNull X column) {
		return new FunctionColumn<>(column.table.internalAlias(""), column, "length(", ")", LONG_PARSER, true, null);
	}

	// FIXME: 8.03.16 add more informative javadoc

	/**
	 * The lower(X) function returns a copy of string X with all ASCII characters converted to lower case.
	 * The default built-in lower() function works for ASCII characters only. To do case conversions
	 * on non-ASCII characters, load the ICU extension.
	 *
	 * @param column Input of this function
	 * @return Column representing the result of this function
	 * @see <a href="http://www.sqlite.org/lang_corefunc.html">SQLite documentation: Core Functions</a>
	 */
	@NonNull
	@CheckResult
	public static <P, X extends Column<?, ?, ? extends CharSequence, P>> Column<String, String, CharSequence, P> lower(@NonNull X column) {
		return new FunctionColumn<>(column.table.internalAlias(""), column, "lower(", ")", STRING_PARSER, true, null);
	}

	/**
	 * The upper(X) function returns a copy of input string X in which all lower-case ASCII characters
	 * are converted to their upper-case equivalent..
	 *
	 * @param column Input of this function
	 * @return Column representing the result of this function
	 * @see <a href="http://www.sqlite.org/lang_corefunc.html">SQLite documentation: Core Functions</a>
	 */
	@NonNull
	@CheckResult
	public static <P, X extends Column<?, ?, ? extends CharSequence, P>> Column<String, String, CharSequence, P> upper(@NonNull X column) {
		return new FunctionColumn<>(column.table.internalAlias(""), column, "upper(", ")", STRING_PARSER, true, null);
	}
}