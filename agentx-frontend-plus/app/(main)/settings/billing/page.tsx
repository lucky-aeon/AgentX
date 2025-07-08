import { CreditCard, Download } from "lucide-react"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card"
import { Progress } from "@/components/ui/progress"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"

export default function BillingPage() {
  return (
    <div className="container py-6">
      <div className="mb-6">
        <h1 className="text-3xl font-bold tracking-tight">账单与用量</h1>
        <p className="text-muted-foreground">管理您的订阅和查看使用情况</p>
      </div>

      <Tabs defaultValue="usage" className="space-y-6">
        <TabsList>
          <TabsTrigger value="usage">使用情况</TabsTrigger>
          <TabsTrigger value="billing">账单历史</TabsTrigger>
          <TabsTrigger value="subscription">订阅管理</TabsTrigger>
        </TabsList>

        <TabsContent value="usage">
          <div className="grid gap-6">
            <Card>
              <CardHeader>
                <CardTitle>Token 使用情况</CardTitle>
                <CardDescription>本月使用的 Token 数量</CardDescription>
              </CardHeader>
              <CardContent className="space-y-6">
                <div className="space-y-2">
                  <div className="flex items-center justify-between">
                    <span className="text-sm font-medium">已使用: 125,000 / 500,000 Tokens</span>
                    <span className="text-sm text-muted-foreground">25%</span>
                  </div>
                  <Progress value={25} className="h-2" />
                </div>

                <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                  <Card>
                    <CardHeader className="pb-2">
                      <CardTitle className="text-base">输入 Tokens</CardTitle>
                    </CardHeader>
                    <CardContent>
                      <div className="text-2xl font-bold">75,000</div>
                      <p className="text-xs text-muted-foreground">占总用量的 60%</p>
                    </CardContent>
                  </Card>
                  <Card>
                    <CardHeader className="pb-2">
                      <CardTitle className="text-base">输出 Tokens</CardTitle>
                    </CardHeader>
                    <CardContent>
                      <div className="text-2xl font-bold">50,000</div>
                      <p className="text-xs text-muted-foreground">占总用量的 40%</p>
                    </CardContent>
                  </Card>
                  <Card>
                    <CardHeader className="pb-2">
                      <CardTitle className="text-base">预计费用</CardTitle>
                    </CardHeader>
                    <CardContent>
                      <div className="text-2xl font-bold">¥125.00</div>
                      <p className="text-xs text-muted-foreground">基于当前使用量</p>
                    </CardContent>
                  </Card>
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>使用明细</CardTitle>
                <CardDescription>按代理和功能查看使用情况</CardDescription>
              </CardHeader>
              <CardContent>
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>代理/功能</TableHead>
                      <TableHead>输入 Tokens</TableHead>
                      <TableHead>输出 Tokens</TableHead>
                      <TableHead className="text-right">总计</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    <TableRow>
                      <TableCell className="font-medium">AI 助手</TableCell>
                      <TableCell>35,000</TableCell>
                      <TableCell>25,000</TableCell>
                      <TableCell className="text-right">60,000</TableCell>
                    </TableRow>
                    <TableRow>
                      <TableCell className="font-medium">数据分析师</TableCell>
                      <TableCell>20,000</TableCell>
                      <TableCell>15,000</TableCell>
                      <TableCell className="text-right">35,000</TableCell>
                    </TableRow>
                    <TableRow>
                      <TableCell className="font-medium">编程助手</TableCell>
                      <TableCell>15,000</TableCell>
                      <TableCell>8,000</TableCell>
                      <TableCell className="text-right">23,000</TableCell>
                    </TableRow>
                    <TableRow>
                      <TableCell className="font-medium">其他</TableCell>
                      <TableCell>5,000</TableCell>
                      <TableCell>2,000</TableCell>
                      <TableCell className="text-right">7,000</TableCell>
                    </TableRow>
                  </TableBody>
                </Table>
              </CardContent>
            </Card>
          </div>
        </TabsContent>

        <TabsContent value="billing">
          <Card>
            <CardHeader>
              <CardTitle>账单历史</CardTitle>
              <CardDescription>查看您的账单和付款历史</CardDescription>
            </CardHeader>
            <CardContent>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>日期</TableHead>
                    <TableHead>描述</TableHead>
                    <TableHead>金额</TableHead>
                    <TableHead>状态</TableHead>
                    <TableHead className="text-right">操作</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  <TableRow>
                    <TableCell>2024-03-01</TableCell>
                    <TableCell>3月份订阅费用</TableCell>
                    <TableCell>¥499.00</TableCell>
                    <TableCell>已支付</TableCell>
                    <TableCell className="text-right">
                      <Button variant="ghost" size="icon">
                        <Download className="h-4 w-4" />
                      </Button>
                    </TableCell>
                  </TableRow>
                  <TableRow>
                    <TableCell>2024-02-01</TableCell>
                    <TableCell>2月份订阅费用</TableCell>
                    <TableCell>¥499.00</TableCell>
                    <TableCell>已支付</TableCell>
                    <TableCell className="text-right">
                      <Button variant="ghost" size="icon">
                        <Download className="h-4 w-4" />
                      </Button>
                    </TableCell>
                  </TableRow>
                  <TableRow>
                    <TableCell>2024-01-01</TableCell>
                    <TableCell>1月份订阅费用</TableCell>
                    <TableCell>¥499.00</TableCell>
                    <TableCell>已支付</TableCell>
                    <TableCell className="text-right">
                      <Button variant="ghost" size="icon">
                        <Download className="h-4 w-4" />
                      </Button>
                    </TableCell>
                  </TableRow>
                </TableBody>
              </Table>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="subscription">
          <div className="grid gap-6">
            <Card>
              <CardHeader>
                <CardTitle>当前订阅</CardTitle>
                <CardDescription>管理您的订阅计划</CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="flex items-center justify-between">
                  <div>
                    <h3 className="font-medium">专业版</h3>
                    <p className="text-sm text-muted-foreground">每月 ¥499.00，包含 500,000 Tokens</p>
                  </div>
                  <Button variant="outline">升级计划</Button>
                </div>
                <div>
                  <p className="text-sm text-muted-foreground">下次续费日期: 2024-04-01</p>
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>支付方式</CardTitle>
                <CardDescription>管理您的支付方式</CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="flex items-center gap-4">
                  <CreditCard className="h-6 w-6" />
                  <div>
                    <p className="font-medium">Visa 尾号 **** 4242</p>
                    <p className="text-sm text-muted-foreground">过期时间: 12/2025</p>
                  </div>
                </div>
              </CardContent>
              <CardFooter className="flex justify-between">
                <Button variant="outline">更改支付方式</Button>
                <Button variant="destructive">取消订阅</Button>
              </CardFooter>
            </Card>
          </div>
        </TabsContent>
      </Tabs>
    </div>
  )
}

