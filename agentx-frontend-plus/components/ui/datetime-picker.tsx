"use client"

import * as React from "react"
import { format } from "date-fns"
import { zhCN } from "date-fns/locale"
import { Calendar as CalendarIcon, Clock } from "lucide-react"
import { DayPicker } from "react-day-picker"

import { cn } from "@/lib/utils"
import { Button } from "@/components/ui/button"
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"

interface DateTimePickerProps {
  value?: Date
  onChange?: (date: Date | undefined) => void
  placeholder?: string
  disabled?: boolean
  className?: string
  showTime?: boolean
}

export function DateTimePicker({
  value,
  onChange,
  placeholder = "选择日期和时间",
  disabled = false,
  className,
  showTime = true
}: DateTimePickerProps) {
  const [open, setOpen] = React.useState(false)
  const [selectedDate, setSelectedDate] = React.useState<Date | undefined>(value)
  const [timeValue, setTimeValue] = React.useState<string>(
    value ? format(value, "HH:mm") : "09:00"
  )

  React.useEffect(() => {
    setSelectedDate(value)
    if (value) {
      setTimeValue(format(value, "HH:mm"))
    }
  }, [value])

  const handleDateSelect = (date: Date | undefined) => {
    if (date) {
      // 如果显示时间选择器，合并日期和时间
      if (showTime) {
        const [hours, minutes] = timeValue.split(':').map(Number)
        const newDate = new Date(date)
        newDate.setHours(hours, minutes, 0, 0)
        setSelectedDate(newDate)
        onChange?.(newDate)
      } else {
        setSelectedDate(date)
        onChange?.(date)
        setOpen(false)
      }
    }
  }

  const handleTimeChange = (time: string) => {
    setTimeValue(time)
    if (selectedDate) {
      const [hours, minutes] = time.split(':').map(Number)
      const newDate = new Date(selectedDate)
      newDate.setHours(hours, minutes, 0, 0)
      setSelectedDate(newDate)
      onChange?.(newDate)
    }
  }

  const handleConfirm = () => {
    setOpen(false)
  }

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <Button
          variant="outline"
          className={cn(
            "w-full justify-start text-left font-normal",
            !selectedDate && "text-muted-foreground",
            className
          )}
          disabled={disabled}
        >
          <CalendarIcon className="mr-2 h-4 w-4" />
          {selectedDate ? (
            showTime ? 
              format(selectedDate, "yyyy-MM-dd HH:mm", { locale: zhCN }) :
              format(selectedDate, "yyyy-MM-dd", { locale: zhCN })
          ) : (
            <span>{placeholder}</span>
          )}
        </Button>
      </PopoverTrigger>
      <PopoverContent className="w-auto p-0" align="start">
        <div className="p-3">
          <DayPicker
            mode="single"
            selected={selectedDate}
            onSelect={handleDateSelect}
            locale={zhCN}
            className="rounded-md border-0"
            classNames={{
              months: "flex flex-col sm:flex-row space-y-4 sm:space-x-4 sm:space-y-0",
              month: "space-y-4",
              caption: "flex justify-center pt-1 relative items-center",
              caption_label: "text-sm font-medium",
              nav: "space-x-1 flex items-center",
              nav_button: cn(
                "h-7 w-7 bg-transparent p-0 opacity-50 hover:opacity-100"
              ),
              nav_button_previous: "absolute left-1",
              nav_button_next: "absolute right-1",
              table: "w-full border-collapse space-y-1",
              head_row: "flex",
              head_cell: "text-muted-foreground rounded-md w-9 font-normal text-[0.8rem]",
              row: "flex w-full mt-2",
              cell: "h-9 w-9 text-center text-sm p-0 relative [&:has([aria-selected].day-range-end)]:rounded-r-md [&:has([aria-selected].day-outside)]:bg-accent/50 [&:has([aria-selected])]:bg-accent first:[&:has([aria-selected])]:rounded-l-md last:[&:has([aria-selected])]:rounded-r-md focus-within:relative focus-within:z-20",
              day: cn(
                "h-9 w-9 p-0 font-normal aria-selected:opacity-100 hover:bg-accent hover:text-accent-foreground rounded-md"
              ),
              day_range_end: "day-range-end",
              day_selected: "bg-primary text-primary-foreground hover:bg-primary hover:text-primary-foreground focus:bg-primary focus:text-primary-foreground",
              day_today: "bg-accent text-accent-foreground",
              day_outside: "day-outside text-muted-foreground opacity-50 aria-selected:bg-accent/50 aria-selected:text-muted-foreground aria-selected:opacity-30",
              day_disabled: "text-muted-foreground opacity-50",
              day_range_middle: "aria-selected:bg-accent aria-selected:text-accent-foreground",
              day_hidden: "invisible",
            }}
          />
          
          {showTime && (
            <div className="border-t pt-3 mt-3">
              <div className="flex items-center space-x-2">
                <Clock className="h-4 w-4 text-muted-foreground" />
                <Label htmlFor="time" className="text-sm">时间</Label>
                <Input
                  id="time"
                  type="time"
                  value={timeValue}
                  onChange={(e) => handleTimeChange(e.target.value)}
                  className="w-32"
                />
              </div>
              <div className="flex justify-end mt-3">
                <Button size="sm" onClick={handleConfirm}>
                  确认
                </Button>
              </div>
            </div>
          )}
        </div>
      </PopoverContent>
    </Popover>
  )
} 