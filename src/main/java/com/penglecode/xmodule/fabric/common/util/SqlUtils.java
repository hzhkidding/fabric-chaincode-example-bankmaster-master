package com.penglecode.xmodule.fabric.common.util;

import com.alibaba.fastjson.JSONObject;
import com.penglecode.xmodule.fabric.bankmaster.Entity.DeleteEntity;
import com.penglecode.xmodule.fabric.bankmaster.Entity.InsertEntity;
import com.penglecode.xmodule.fabric.bankmaster.Entity.SelectEntity;
import com.penglecode.xmodule.fabric.bankmaster.Entity.UpdateEntity;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.util.TablesNamesFinder;
import net.sf.jsqlparser.util.deparser.ExpressionDeParser;

import java.util.*;

public class SqlUtils {
    public static Object sqlParser(String sql,String params) throws JSQLParserException {
        com.alibaba.fastjson.JSONObject jsonParams = (com.alibaba.fastjson.JSONObject) com.alibaba.fastjson.JSONObject.parse(params);
        StringBuffer preSql = new StringBuffer(sql);
        StringBuffer endSql = new StringBuffer();
        for(int i =0,index =1 ;i<preSql.length();i++){
            if(preSql.charAt(i) == '?'){
                endSql.append(jsonParams.getString(String.valueOf(index)));
                index++;
            }
            else  endSql.append(preSql.charAt(i));
        }
        System.out.println(endSql);
        sql = String.valueOf(endSql);

        Object returnSqlObject = CCJSqlParserUtil.parse(sql);

      // Select
        if(returnSqlObject.getClass() == Select.class){
            Map<String,String> whereFields = new HashMap<>();
            SelectEntity selectEntity = new SelectEntity();
            Select select = (Select) returnSqlObject;
            SelectBody selectBody = select.getSelectBody();
            PlainSelect plainSelect = (PlainSelect)selectBody;
            TablesNamesFinder tablesNamesFinder = new TablesNamesFinder();
            List<String> result = tablesNamesFinder.getTableList(select);
            String tableName = result.get(0);
            List selectFieldsPre = ((PlainSelect) selectBody).getSelectItems();
            String[] splitSelect = selectFieldsPre.get(0).toString().split(" ");
            List<String> selectFields  = new ArrayList<>();
            for(int i =0;i< splitSelect.length;i++){
                selectFields.add(splitSelect[i]);
            }
            if(plainSelect.getWhere()  instanceof AndExpression){
                AndExpression andExpression = (AndExpression) plainSelect.getWhere();
                System.out.println(andExpression.toString());
                EqualsTo equalsTo = (EqualsTo) andExpression.getLeftExpression();
                whereFields.put(((Column)equalsTo.getLeftExpression()).getColumnName(),String.valueOf(equalsTo.getRightExpression()));
                equalsTo = (EqualsTo)andExpression.getRightExpression();
                whereFields.put(((Column)equalsTo.getLeftExpression()).getColumnName(),String.valueOf(equalsTo.getRightExpression()));
                selectEntity.setTableName(tableName);
                selectEntity.setWhereFields(whereFields);
            }else  if(plainSelect.getWhere() instanceof Expression) {
                Expression expression = plainSelect.getWhere();
                ExpressionDeParser expressionDeParser = new ExpressionDeParser();
                plainSelect.getWhere().accept(expressionDeParser);
                EqualsTo equalsTo = (EqualsTo)expression;

                whereFields.put(((Column)equalsTo.getLeftExpression()).getColumnName(), String.valueOf(equalsTo.getRightExpression()));
                selectEntity.setWhereFields(whereFields);
                selectEntity.setTableName(tableName);
                System.out.println(tableName);
                System.out.println("Field:"+((Column)equalsTo.getLeftExpression()).getColumnName());
                System.out.println("equal:"+equalsTo.getRightExpression());
            }else
                selectEntity.setTableName(tableName);
            selectEntity.setSelectFields(selectFields);
            return  selectEntity;
        }
        // Insert
        if(returnSqlObject.getClass() == Insert.class){
            InsertEntity insertEntity = new InsertEntity();
            Insert insert = (Insert) returnSqlObject;
            List<Column> list = insert.getColumns();
            TablesNamesFinder tablesNamesFinder = new TablesNamesFinder();
            List<String> result = tablesNamesFinder.getTableList(insert);

            Iterator iteratorKey = list.iterator();
            ExpressionList expressionList = (ExpressionList) insert.getItemsList();
            int index = 0;
            JSONObject jsonObject = new JSONObject();
            while(iteratorKey.hasNext()){
                Column column = (Column) iteratorKey.next();
                System.out.println(expressionList.getExpressions().get(index));
                jsonObject.put(column.getColumnName(),String.valueOf(expressionList.getExpressions().get(index)));
                index++;
            }
            System.out.println(jsonObject.toString());
            insertEntity.setTableName(result.get(0));
            insertEntity.setJsonParams(jsonObject);
            return insertEntity;
        }


        // UPDATE
        if(returnSqlObject.getClass() == Update.class){
            UpdateEntity updateEntity = new UpdateEntity();
            Map<String,String> setFields = new HashMap<>();
            Map<String,String> whereFields = new HashMap<>();
            Update update = (Update) returnSqlObject;
            System.out.println(update.getColumns().toString());
            List<Column> property =update.getColumns();
            List<?> value = update.getExpressions();
            for(int i = 0 ;i<property.size();i++){
                setFields.put(property.get(i).getColumnName(),value.get(i).toString());
            }
            if(update.getWhere() != null){
                EqualsTo equalsTo = (EqualsTo) update.getWhere();
                whereFields.put(equalsTo.getLeftExpression().toString(),equalsTo.getRightExpression().toString());
            }
            updateEntity.setWhereFields(whereFields);
            updateEntity.setSetFields(setFields);
            updateEntity.setTableName(String.valueOf(update.getTables().get(0)));
             return  updateEntity;
        }


        //delete
        if(returnSqlObject.getClass() == Delete.class){
            Map<String,String> whereFields  = new HashMap<>();
            DeleteEntity deleteEntity = new DeleteEntity();
            Delete delete = (Delete) returnSqlObject;
            TablesNamesFinder tablesNamesFinder = new TablesNamesFinder();
            List<String> result = tablesNamesFinder.getTableList(delete);
            deleteEntity.setTableName(result.get(0));
            if(delete.getWhere() instanceof  AndExpression){
                AndExpression andExpression = (AndExpression) delete.getWhere();
                EqualsTo equalsTo = (EqualsTo) andExpression.getLeftExpression();
                whereFields.put(((Column)equalsTo.getLeftExpression()).getColumnName(),String.valueOf(equalsTo.getRightExpression()));
                equalsTo = (EqualsTo)andExpression.getRightExpression();
                whereFields.put(((Column)equalsTo.getLeftExpression()).getColumnName(),String.valueOf(equalsTo.getRightExpression()));
                System.out.println(whereFields.toString());
                deleteEntity.setWhereFields(whereFields);
                deleteEntity.setTableName(delete.getTable().getName());
                return deleteEntity;
            } else if(delete.getWhere() instanceof  Expression){
                Expression expression = delete.getWhere();
             //   ExpressionDeParser expressionDeParser = new ExpressionDeParser();
                EqualsTo equalsTo = (EqualsTo)expression;
                whereFields.put(((Column)equalsTo.getLeftExpression()).getColumnName(), String.valueOf(equalsTo.getRightExpression()));
                deleteEntity.setWhereFields(whereFields);
                deleteEntity.setTableName(delete.getTable().getName());
                System.out.println(delete.getTable().getName());
                System.out.println("Field:"+((Column)equalsTo.getLeftExpression()).getColumnName());
                System.out.println("equal:"+equalsTo.getRightExpression());
                return  deleteEntity;
            }
        }
        return  "error";
    }
}
