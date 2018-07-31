package org.flowable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.impl.cfg.StandaloneProcessEngineConfiguration;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.task.Task;

/**
 * 报销流程
 * 
 * @author ZhangXiang
 *
 */
public class ExpenseRequest {

	/**
	 * 主函数
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		// 配置数据源（存储过程数据和历史数据）
		ProcessEngineConfiguration cfg = new StandaloneProcessEngineConfiguration()
				.setJdbcUrl("jdbc:h2:mem:flowable;DB_CLOSE_DELAY=-1").setJdbcUsername("sa").setJdbcPassword("")
				.setJdbcDriver("org.h2.Driver")
				.setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);

		// 实例化过程引擎实例
		ProcessEngine processEngine = cfg.buildProcessEngine();
		RepositoryService repositoryService = processEngine.getRepositoryService();
		Deployment deployment = repositoryService.createDeployment()
				.addClasspathResource("processes/expense-request.bpmn20.xml").deploy();
		ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
				.deploymentId(deployment.getId()).singleResult();
		System.out.println("Found process definition : " + processDefinition.getName());

		// 输出传递的流程变量taskUser和money
		Scanner scanner = new Scanner(System.in);
		String taskUser = "";
		Integer money = 0;
		while (taskUser == "" || taskUser == null || money == 0) {
			System.out.println("who are you?");
			String t1 = scanner.nextLine();
			if (t1 != null && !t1.equals("")) {
				taskUser = t1;
			}
			System.out.println("how much is your expenses?");
			String t2 = scanner.nextLine();
			if (t2 != null && !t2.equals("")) {
				money = Integer.valueOf(t2);
			}
		}

		// 实例化一个流程
		RuntimeService runtimeService = processEngine.getRuntimeService();
		Map<String, Object> variables = new HashMap<String, Object>();
		variables.put("taskUser", taskUser);
		variables.put("money", money);
		ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("expenseRequest", variables);
		System.out.println("The process starts successfully! Id is" + processInstance.getId());

		// 得到报销人的任务列表
		TaskService taskService = processEngine.getTaskService();
		List<Task> tasks = taskService.createTaskQuery().taskAssignee(taskUser).list();
		System.out.println("You have " + tasks.size() + " tasks:");
		for (Task task : tasks) {
			System.out.println(task.toString());
		}

		// 完成任务
		System.out.println("Which task would you like to complete?");
		int taskIndex = Integer.valueOf(scanner.nextLine());
		Task task = tasks.get(taskIndex - 1);
		Map<String, Object> processVariables = taskService.getVariables(task.getId());
		System.out.println(processVariables.get("taskUser") + " wants " + processVariables.get("money")
				+ ". Do you approve this?please press 通过 or 驳回");
		String approved = scanner.nextLine();
		if (approved == null || !approved.equals("通过")) {
			approved = "驳回";
		}
		variables = new HashMap<String, Object>();
		variables.put("outcome", approved);
		taskService.complete(task.getId(), variables);
		System.out.println("finshed!" + approved + task.getAssignee() + "'s expense");
		scanner.close();
	}
}
