package testing

import testing.annotations.*

import java.lang.reflect.InvocationTargetException
import scala.util.control.NonFatal

class AssertionException(msg: String) extends RuntimeException(msg)

object Checkers:
  def assertEquals(expected: Any, actual: Any): Unit =
    if expected != actual then
      throw new AssertionException(s"assertEquals failure (${expected.toString} != ${actual.toString})")

object TestRunner:
  def main(args: Array[String]): Unit =
    if args.isEmpty then
      println("Ошибка: необходимо передать полное имя класса для тестирования.")
      sys.exit(1)

    val className = args(0)

    try {
      val clazz = Class.forName(className)

      val constructor = clazz.getDeclaredConstructor()
      val instance = constructor.newInstance()

      val methods = clazz.getDeclaredMethods

      val beforeMethod = methods.find(_.isAnnotationPresent(classOf[Before]))
      val afterMethod = methods.find(_.isAnnotationPresent(classOf[After]))

      val testMethods = methods.filter(_.isAnnotationPresent(classOf[Test]))

      if testMethods.isEmpty then
        println(s"В классе $className не найдено методов с аннотацией @annotations.Test")
        return

      for test <- testMethods do
        if test.getParameterCount > 0 then
          println(s"FAILED: ${test.getName}; REASON: метод теста не должен иметь параметров")
        else
          var testFailed = false
          var failureReason = ""

          try {
            beforeMethod.foreach(_.invoke(instance))

            val expectedEx = test.getAnnotation(classOf[ExpectedException])
            if expectedEx != null then
              testFailed = true
              failureReason = s"expected exception class ${expectedEx.value().getName} does not throw"

            try {
              test.invoke(instance)
            } catch {
              case e: InvocationTargetException =>
                val cause = e.getCause

                if (expectedEx != null) {
                  // Если исключение ожидалось, проверяем его тип
                  if !expectedEx.value().isAssignableFrom(cause.getClass) then
                    testFailed = true
                    failureReason = s"unexpected exception: class ${cause.getClass.getName}"
                } else {
                  // Если исключение не ожидалось
                  testFailed = true
                  cause match
                    case ae: AssertionException => failureReason = ae.getMessage
                    case _ => failureReason = s"unexpected exception: class ${cause.getClass.getName}"
                }
            }
          } finally {
            // Гарантированно вызываем After (если есть), даже если тест упал
            try
              afterMethod.foreach(_.invoke(instance))
            catch
              case NonFatal(e) =>
                testFailed = true
                failureReason += s" (также ошибка в @After: ${e.getCause.getMessage})"
          }

          if testFailed then
            println(s"FAILED: ${test.getName}; REASON: $failureReason")
          else
            println(s"PASSED: ${test.getName}")
    } catch {
      case _: ClassNotFoundException =>
        println(s"Ошибка: Класс '$className' не найден.")
      case _: NoSuchMethodException =>
        println(s"Ошибка: В классе '$className' нет публичного конструктора без параметров.")
      case NonFatal(e) =>
        println(s"Критическая ошибка при запуске тестов: ${e.getMessage}")
    }