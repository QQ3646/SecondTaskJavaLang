package test

import testing.*
import testing.annotations.*

class MyTests:

  @Before def setUp(): Unit = ()
  @After def tearDown(): Unit = ()

  @Test
  def goodTest(): Unit =
    Checkers.assertEquals(42, 42)

  @Test
  def badTest(): Unit =
    Checkers.assertEquals(1, 2)

  @Test
  @ExpectedException(classOf[IllegalArgumentException])
  def goodTestWithException(): Unit =
    throw new IllegalArgumentException("Bad arg")

  @Test
  @ExpectedException(classOf[NullPointerException])
  def badTestWithException(): Unit =
    val a = 1 + 1

  @Test
  def testWithUnexpectedException(): Unit =
    throw new IllegalStateException("Wrong state")

